package com.fireway.backend.modules.routing.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fireway.backend.modules.routing.application.port.*;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.fireway.backend.modules.routing.domain.Coordinate;
import com.fireway.backend.shared.exception.ExternalSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// OSRM road candidates for deployments without Valhalla; never fabricate geometry or ETA.
@Component
@Profile("!valhalla")
public class MockValhallaClient implements ValhallaClient {
    private static final Logger log = LoggerFactory.getLogger(MockValhallaClient.class);
    private final String baseUrl;
    public MockValhallaClient(@org.springframework.beans.factory.annotation.Value("${OSRM_URL:https://router.project-osrm.org}") String baseUrl) {
        this.baseUrl = baseUrl;
    }
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public List<RouteCandidate> route(RoutePlanRequest request) {
        List<Coordinate> vias = request.cleared().stream()
                .sorted(Comparator.comparingDouble(p -> {
                    double[] point = p.pathAsLonLat().get(p.pathAsLonLat().size() / 2);
                    return Math.hypot(point[0] - request.to().lon(), point[1] - request.to().lat());
                }))
                .limit(6).map(p -> p.pathAsLonLat().get(p.pathAsLonLat().size() / 2))
                .map(p -> new Coordinate(p[1], p[0])).toList();
        List<CompletableFuture<List<RouteCandidate>>> searches = new ArrayList<>();
        searches.add(search(request, null));
        // Bounded candidate search via CCTV-cleared roads; only real OSRM geometries are returned.
        for (Coordinate via : vias) searches.add(search(request, via));
        List<RouteCandidate> candidates = new ArrayList<>();
        int completed = 0;
        for (var search : searches) {
            try { candidates.addAll(search.join()); completed++; }
            catch (RuntimeException error) { log.warn("OSRM route search failed: {}", error.getMessage()); }
        }
        if (completed == 0) throw new ExternalSystemException("도로 경로 조회에 실패했습니다.");
        return candidates;
    }

    private CompletableFuture<List<RouteCandidate>> search(RoutePlanRequest request, Coordinate via) {
        return CompletableFuture.supplyAsync(() -> {
            try { return osrmRoute(request, via); }
            catch (Exception error) { throw new java.util.concurrent.CompletionException(error); }
        });
    }

    private List<RouteCandidate> osrmRoute(RoutePlanRequest request, Coordinate via) throws Exception {
        String coords = String.format(Locale.ROOT, "%f,%f;", request.from().lon(), request.from().lat())
                + (via == null ? "" : String.format(Locale.ROOT, "%f,%f;", via.lon(), via.lat()))
                + String.format(Locale.ROOT, "%f,%f", request.to().lon(), request.to().lat());
        URI uri = URI.create(baseUrl + "/route/v1/driving/" + coords
                + "?overview=full&geometries=geojson&alternatives=true");
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(6)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("OSRM " + res.statusCode());
        JsonNode root = JSON.readTree(res.body());
        JsonNode routes = root.path("routes");
        if ("NoRoute".equals(root.path("code").asText())) return List.of();
        if (!routes.isArray() || routes.isEmpty()) throw new RuntimeException("no OSRM routes");
        // A waypoint snapped to an adjacent road is not evidence of traversing the cleared alley.
        if (via != null && root.path("waypoints").path(1).path("distance").asDouble(Double.MAX_VALUE) > 20)
            return List.of();
        List<RouteCandidate> candidates = new ArrayList<>();
        int rank = 1;
        for (JsonNode r : routes) {
            if (rank > request.k()) break;
            List<double[]> pts = new ArrayList<>();
            for (JsonNode c : r.path("geometry").path("coordinates")) {
                pts.add(new double[]{c.get(0).asDouble(), c.get(1).asDouble()});
            }
            if (pts.size() < 2) continue;
            double duration = r.path("duration").asDouble(Double.NaN);
            double distanceM = r.path("distance").asDouble(Double.NaN);
            if (!Double.isFinite(duration) || duration < 0 || duration > Integer.MAX_VALUE
                    || !Double.isFinite(distanceM) || distanceM < 0)
                throw new IllegalArgumentException("Invalid OSRM summary");
            int etaSec = (int) Math.ceil(duration);
            candidates.add(new RouteCandidate(rank++, pts, PolylineCodec.encode(pts), etaSec, distanceM));
        }
        return candidates;
    }
}
