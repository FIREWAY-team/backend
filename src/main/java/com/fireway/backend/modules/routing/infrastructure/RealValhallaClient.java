package com.fireway.backend.modules.routing.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fireway.backend.modules.routing.application.port.*;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.time.Duration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Profile("valhalla")
public class RealValhallaClient implements ValhallaClient {
    private static final Logger log = LoggerFactory.getLogger(RealValhallaClient.class);
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
                "exclude_polygons", request.polygons().stream().map(p -> com.fireway.backend.modules.routing.application.RouteGeometry.exclusionRing(p.pathAsLonLat())).toList(),
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
            log.warn("Valhalla 경로 조회 실패 · 시연 폴백 직선 반환: {}", error.getMessage(), error);
            // ponytail: 자체 Valhalla 안정화되면 이 폴백 제거하고 ExternalSystemException 로 복귀.
            return List.of(fallbackStraightLine(request));
        }
    }

    private static RouteCandidate fallbackStraightLine(RoutePlanRequest request) {
        double fromLon = request.from().lon(), fromLat = request.from().lat();
        double toLon = request.to().lon(), toLat = request.to().lat();
        List<double[]> pts = List.of(new double[]{fromLon, fromLat}, new double[]{toLon, toLat});
        double dLat = Math.toRadians(toLat - fromLat), dLon = Math.toRadians(toLon - fromLon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double distanceM = 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        int etaSec = (int) Math.ceil(distanceM / (40.0 * 1000 / 3600));
        RouteCandidate rc = new RouteCandidate(1, pts, PolylineCodec.encode(pts), etaSec, distanceM);
        rc.setExplanation("라우터 폴백 · 실 도로 아님 (Valhalla 응답 없음)");
        return rc;
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
