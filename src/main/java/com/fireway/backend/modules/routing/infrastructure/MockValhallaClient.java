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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Valhalla 컨테이너가 없는 환경(시연 서버 포함)의 기본 라우터. 이름은 Mock 이지만 실제로는
// project-osrm.org 공개 API 로 도로 기반 경로를 뽑는다 — 폴백만 옛 mock 로직(직선 3점).
// SPRING_PROFILES_ACTIVE=prod,valhalla 로 켤 때만 RealValhallaClient 가 등록된다.
@Component
@Profile("!valhalla")
public class MockValhallaClient implements ValhallaClient {
    private static final Logger log = LoggerFactory.getLogger(MockValhallaClient.class);
    private static final String OSRM_BASE = "https://router.project-osrm.org";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public List<RouteCandidate> route(RoutePlanRequest request) {
        try {
            return osrmRoute(request);
        } catch (Exception e) {
            log.warn("OSRM 경로 조회 실패, 직선 폴백: {}", e.getMessage());
            return straightLineFallback(request);
        }
    }

    private List<RouteCandidate> osrmRoute(RoutePlanRequest request) throws Exception {
        String coords = String.format(Locale.ROOT, "%f,%f;%f,%f",
                request.from().lon(), request.from().lat(),
                request.to().lon(), request.to().lat());
        URI uri = URI.create(OSRM_BASE + "/route/v1/driving/" + coords
                + "?overview=full&geometries=geojson&alternatives=true");
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(6)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("OSRM " + res.statusCode());
        JsonNode root = JSON.readTree(res.body());
        JsonNode routes = root.path("routes");
        if (!routes.isArray() || routes.isEmpty()) throw new RuntimeException("no OSRM routes");
        List<RouteCandidate> candidates = new ArrayList<>();
        int rank = 1;
        for (JsonNode r : routes) {
            if (rank > request.k()) break;
            List<double[]> pts = new ArrayList<>();
            for (JsonNode c : r.path("geometry").path("coordinates")) {
                pts.add(new double[]{c.get(0).asDouble(), c.get(1).asDouble()});
            }
            if (pts.size() < 2) continue;
            int etaSec = (int) Math.ceil(r.path("duration").asDouble());
            double distanceM = r.path("distance").asDouble();
            candidates.add(new RouteCandidate(rank++, pts, PolylineCodec.encode(pts), etaSec, distanceM));
        }
        // OSRM 공개 서버는 성남 좁은 골목에서 alternatives 를 1개만 주는 게 일반적 —
        // 부족하면 첫 후보를 살짝 흔들어 UX 상 후보 카드 3개 유지.
        while (candidates.size() < request.k() && !candidates.isEmpty()) {
            RouteCandidate first = candidates.get(0);
            int i = candidates.size();
            int etaSec = first.etaSec() + 80 * i;
            double distanceM = first.distanceM() * (1 + 0.15 * i);
            candidates.add(new RouteCandidate(candidates.size() + 1,
                    first.coordinates(), first.polyline(), etaSec, distanceM));
        }
        return candidates;
    }

    private List<RouteCandidate> straightLineFallback(RoutePlanRequest request) {
        double lon = request.from().lon(), lat = request.from().lat();
        double dx = request.to().lon() - lon, dy = request.to().lat() - lat;
        double distance = 111_320 * Math.hypot(dx * Math.cos(Math.toRadians((lat + request.to().lat()) / 2)), dy);
        List<RouteCandidate> candidates = new ArrayList<>();
        int[] etas = {200, 280, 340};
        for (int i = 0; i < request.k(); i++) {
            double jitter = (i - 1) * 0.12;
            List<double[]> points = List.of(new double[]{lon, lat},
                    new double[]{lon + dx * 0.5 - dy * jitter, lat + dy * 0.5 + dx * jitter},
                    new double[]{request.to().lon(), request.to().lat()});
            candidates.add(new RouteCandidate(i + 1, points, PolylineCodec.encode(points),
                    etas[Math.min(i, etas.length - 1)], distance * (1 + i * 0.15)));
        }
        return candidates;
    }
}
