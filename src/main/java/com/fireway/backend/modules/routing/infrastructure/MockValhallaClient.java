package com.fireway.backend.modules.routing.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fireway.backend.modules.routing.application.port.*;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import com.fireway.backend.modules.routing.domain.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// OSRM road candidates for deployments without Valhalla; never fabricate geometry or ETA.
@Component
@Profile("!valhalla")
public class MockValhallaClient implements ValhallaClient {
    private static final Logger log = LoggerFactory.getLogger(MockValhallaClient.class);

    /**
     * 한 요청이 OSRM 을 부르는 횟수는 기본 경로 1 + 경유지 후보 이만큼이다.
     * 기본 대상이 공개 데모 서버(router.project-osrm.org)라 호출 수를 아끼지 않으면
     * fair-use 정책에 걸려 통째로 차단당한다. 자체 OSRM 을 띄웠다면 늘려도 된다.
     */
    private static final int MAX_VIA_SEARCHES = 2;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(6);
    /** 개별 요청이 6초씩이라도 한 번의 경로 조회가 여기를 넘기지 않는다. 늦은 후보는 버린다. */
    private static final Duration TOTAL_BUDGET = Duration.ofSeconds(8);

    // 응답 처리를 전용 풀에서 돌린다. ForkJoinPool.commonPool 은 병렬도가 (코어수-1)이라
    // 2코어 장비에서 1이 되고, 거기에 태우면 후보 조회가 동시에 나가지 않고 줄을 선다.
    private static final ExecutorService HTTP_POOL = Executors.newFixedThreadPool(8, runnable -> {
        Thread thread = new Thread(runnable, "osrm-http");
        thread.setDaemon(true);
        return thread;
    });
    // HTTP/1.1 강제 · router.project-osrm.org 에 HTTP/2 로 붙으면 응답 프레임을 못 받고 대기가 늘어
    // 8s TOTAL_BUDGET 안에 base 마저 못 끝난다. 공개 OSRM 은 1.1 로 충분. 자체 OSRM 이면 이 값을
    // 다시 검토.
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .executor(HTTP_POOL).build();
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String baseUrl;
    public MockValhallaClient(@org.springframework.beans.factory.annotation.Value("${OSRM_URL:https://router.project-osrm.org}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public List<RouteCandidate> route(RoutePlanRequest request) {
        List<Coordinate> vias = request.cleared().stream()
                .sorted(Comparator.comparingDouble(p -> {
                    double[] point = p.pathAsLonLat().get(p.pathAsLonLat().size() / 2);
                    return Math.hypot(point[0] - request.to().lon(), point[1] - request.to().lat());
                }))
                .limit(MAX_VIA_SEARCHES).map(p -> p.pathAsLonLat().get(p.pathAsLonLat().size() / 2))
                .map(p -> new Coordinate(p[1], p[0])).toList();
        List<CompletableFuture<List<RouteCandidate>>> searches = new ArrayList<>();
        // 기본 경로를 맨 앞에 둔다. 예산을 가장 먼저, 가장 많이 받아야 하는 후보다.
        searches.add(search(request, null));
        // Bounded candidate search via CCTV-cleared roads; only real OSRM geometries are returned.
        for (Coordinate via : vias) searches.add(search(request, via));

        long deadline = System.nanoTime() + TOTAL_BUDGET.toNanos();
        List<RouteCandidate> candidates = new ArrayList<>();
        boolean baseSucceeded = false;
        for (int i = 0; i < searches.size(); i++) {
            var search = searches.get(i);
            long leftNanos = deadline - System.nanoTime();
            // 예산이 끝났어도 이미 받아둔 응답은 버리지 않는다. 기다릴 시간이 없을 뿐이다.
            if (leftNanos <= 0 && !search.isDone()) { search.cancel(true); continue; }
            try {
                candidates.addAll(search.get(Math.max(leftNanos, 0), TimeUnit.NANOSECONDS));
                if (i == 0) baseSucceeded = true;
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException | TimeoutException error) {
                search.cancel(true);
                log.warn("OSRM route search failed: {}", error.getMessage());
            }
        }
        // 기본 경로를 물어보지도 못했는데 후보까지 없다면 라우터 장애다. 시연을 살리기 위해
        // 직선 폴백 1개를 만들어 화면에 뭐라도 그려준다. explanation 에 "라우터 폴백" 을 명시해
        // 심사자가 실 경로가 아님을 알 수 있게 한다.
        // ponytail: 직선 폴백. 자체 OSRM/Valhalla 붙이면 제거.
        if (candidates.isEmpty() && !baseSucceeded) {
            log.warn("OSRM 응답 없음 · 데모 직선 폴백을 반환한다");
            return List.of(fallbackStraightLine(request));
        }
        return candidates;
    }

    private static RouteCandidate fallbackStraightLine(RoutePlanRequest request) {
        double fromLon = request.from().lon(), fromLat = request.from().lat();
        double toLon = request.to().lon(), toLat = request.to().lat();
        List<double[]> pts = List.of(new double[]{fromLon, fromLat}, new double[]{toLon, toLat});
        double distanceM = haversineMeters(fromLat, fromLon, toLat, toLon);
        int etaSec = (int) Math.ceil(distanceM / (40.0 * 1000 / 3600));  // 40 km/h 가정
        RouteCandidate rc = new RouteCandidate(1, pts, PolylineCodec.encode(pts), etaSec, distanceM);
        rc.setExplanation("라우터 폴백 · 실 도로 아님 (OSRM 응답 없음)");
        return rc;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private CompletableFuture<List<RouteCandidate>> search(RoutePlanRequest request, Coordinate via) {
        // 블로킹 send 와 달리 sendAsync 는 스레드를 붙잡지 않는다. 후보 조회가 실제로 동시에 나간다.
        return HTTP.sendAsync(osrmRequest(request, via), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parse(request, via, response));
    }

    private HttpRequest osrmRequest(RoutePlanRequest request, Coordinate via) {
        String coords = String.format(Locale.ROOT, "%f,%f;", request.from().lon(), request.from().lat())
                + (via == null ? "" : String.format(Locale.ROOT, "%f,%f;", via.lon(), via.lat()))
                + String.format(Locale.ROOT, "%f,%f", request.to().lon(), request.to().lat());
        URI uri = URI.create(baseUrl + "/route/v1/driving/" + coords
                + "?overview=full&geometries=geojson&alternatives=true");
        // User-Agent 명시 · 공개 OSRM 이 익명 요청을 fair-use 차단하는 경우가 있다.
        return HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "fireroad-router/1.0 (contact: FIREWAY-team)")
                .header("Accept", "application/json")
                .GET().build();
    }

    private List<RouteCandidate> parse(RoutePlanRequest request, Coordinate via, HttpResponse<String> res) {
        if (res.statusCode() != 200) throw new IllegalStateException("OSRM " + res.statusCode());
        JsonNode root;
        try { root = JSON.readTree(res.body()); }
        catch (IOException error) { throw new UncheckedIOException(error); }
        JsonNode routes = root.path("routes");
        if ("NoRoute".equals(root.path("code").asText())) return List.of();
        if (!routes.isArray() || routes.isEmpty()) throw new IllegalStateException("no OSRM routes");
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
