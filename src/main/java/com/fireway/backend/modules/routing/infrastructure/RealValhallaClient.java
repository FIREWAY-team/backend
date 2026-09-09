package com.fireway.backend.modules.routing.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fireway.backend.modules.routing.application.port.*;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import com.fireway.backend.shared.exception.ExternalSystemException;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Profile("prod")
public class RealValhallaClient implements ValhallaClient {
    private final WebClient client;

    public RealValhallaClient(WebClient.Builder builder, @Value("${VALHALLA_URL}") String url) {
        if (url.isBlank()) throw new IllegalArgumentException("VALHALLA_URL must not be blank");
        this.client = builder.baseUrl(url).build();
    }

    @Override
    public List<RouteCandidate> route(RoutePlanRequest request) {
        var vehicle = request.vehicle();
        var truck = new ValhallaTruckOptions(vehicle.widthM(), vehicle.heightM(), vehicle.lengthM(), vehicle.weightTon(), false);
        Map<String, Object> body = Map.of(
                "locations", List.of(Map.of("lat", request.from().lat(), "lon", request.from().lon()),
                        Map.of("lat", request.to().lat(), "lon", request.to().lon())),
                "costing", "truck",
                "costing_options", Map.of("truck", Map.of("width", truck.width(), "height", truck.height(),
                        "length", truck.length(), "weight", truck.weight(), "hazmat", truck.hazmat())),
                "exclude_polygons", request.polygons().stream().map(p -> p.pathAsLonLat()).toList(),
                "alternates", request.k() - 1,
                "directions_options", Map.of("units", "kilometers"));
        try {
            JsonNode response = client.post().uri("/route").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body).retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(10));
            if (response == null || !response.path("trip").isObject()) {
                throw new IllegalArgumentException("Missing Valhalla trip");
            }
            List<RouteCandidate> candidates = new ArrayList<>();
            candidates.add(parseTrip(response.path("trip"), 1));
            for (JsonNode alternate : response.path("alternates")) {
                if (candidates.size() >= request.k()) break;
                candidates.add(parseTrip(alternate.path("trip"), candidates.size() + 1));
            }
            return candidates;
        } catch (RuntimeException error) {
            throw new ExternalSystemException("Valhalla 경로 조회에 실패했습니다.");
        }
    }

    private RouteCandidate parseTrip(JsonNode trip, int rank) {
        JsonNode summary = trip.path("summary");
        if (trip.path("status").asInt(-1) != 0 || !summary.path("time").isNumber()
                || !summary.path("length").isNumber() || !trip.path("legs").isArray()) {
            throw new IllegalArgumentException("Invalid Valhalla trip");
        }
        double time = summary.path("time").asDouble(), distance = summary.path("length").asDouble() * 1000;
        if (!Double.isFinite(time) || time < 0 || time > Integer.MAX_VALUE
                || !Double.isFinite(distance) || distance < 0) throw new IllegalArgumentException("Invalid trip summary");
        List<double[]> points = new ArrayList<>();
        for (JsonNode leg : trip.path("legs")) {
            for (double[] point : PolylineCodec.decode(leg.path("shape").asText())) {
                if (points.isEmpty() || !Arrays.equals(points.get(points.size() - 1), point)) points.add(point);
            }
        }
        if (points.size() < 2) throw new IllegalArgumentException("Missing trip geometry");
        return new RouteCandidate(rank, points, PolylineCodec.encode(points), (int) Math.ceil(time), distance);
    }
}
