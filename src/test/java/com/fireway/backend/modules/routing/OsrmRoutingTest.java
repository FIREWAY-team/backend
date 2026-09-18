package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import com.fireway.backend.modules.routing.application.port.RoutePlanRequest;
import com.fireway.backend.modules.routing.domain.NoGoAreaSummary;
import com.fireway.backend.modules.routing.infrastructure.MockValhallaClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class OsrmRoutingTest {
    @Test
    void searches_via_cctv_pass_alley_without_cloning_alternatives() throws Exception {
        List<String> paths = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/route", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            byte[] body = """
                {"code":"Ok","waypoints":[{"distance":0},{"distance":0},{"distance":0}],
                 "routes":[{"geometry":{"coordinates":[[127.14,37.44],[127.15,37.445],[127.16,37.45]]},"duration":123,"distance":1000}]}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var client = new MockValhallaClient("http://127.0.0.1:" + server.getAddress().getPort());
            var pass = new NoGoAreaSummary("pass", "좁은 골목", null, List.of(new double[]{127.15, 37.445}));
            var result = client.route(new RoutePlanRequest(RoutingTestFixtures.FROM, RoutingTestFixtures.TO,
                    RoutingTestFixtures.VEHICLE, List.of(), 3, List.of(pass)));
            assertThat(paths).hasSize(2).anyMatch(p -> p.contains("127.150000,37.445000;"));
            // One actual response per search; no fabricated 2nd/3rd alternative ETA.
            assertThat(result).hasSize(2).allSatisfy(r -> {
                assertThat(r.etaSec()).isEqualTo(123);
                assertThat(r.distanceM()).isEqualTo(1000);
            });
        } finally { server.stop(0); }
    }

    // 공개 OSRM 데모 서버가 기본값이라, 뚫린 골목이 몇 개든 한 요청의 호출 수는 고정돼야 한다.
    // 예전엔 경유지 6개까지 나가서 한 번 누를 때마다 외부 호출이 7번이었다.
    @Test
    void caps_outgoing_calls_regardless_of_cleared_alley_count() throws Exception {
        List<String> paths = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/route", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            byte[] body = """
                {"code":"Ok","waypoints":[{"distance":0},{"distance":0},{"distance":0}],
                 "routes":[{"geometry":{"coordinates":[[127.14,37.44],[127.16,37.45]]},"duration":123,"distance":1000}]}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var client = new MockValhallaClient("http://127.0.0.1:" + server.getAddress().getPort());
            List<NoGoAreaSummary> cleared = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                cleared.add(new NoGoAreaSummary("pass-" + i, "좁은 골목", null,
                        List.of(new double[]{127.150 + i * 0.001, 37.445})));
            }
            client.route(new RoutePlanRequest(RoutingTestFixtures.FROM, RoutingTestFixtures.TO,
                    RoutingTestFixtures.VEHICLE, List.of(), 3, cleared));
            // 기본 경로 1 + 경유지 2. 뚫린 골목 6개가 그대로 호출 6번이 되면 안 된다.
            assertThat(paths).hasSize(3);
        } finally { server.stop(0); }
    }

    // ponytail: 시연 폴백 도입으로 라우터 장애 시 직선 폴백 1개를 반환하도록 바뀌었다.
    // (§MockValhallaClient · fallbackStraightLine). 자체 OSRM/Valhalla 붙이면 이 테스트를 되살린다.
    @Test
    void unavailable_router_returns_demo_fallback_line() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/route", exchange -> { exchange.sendResponseHeaders(503, -1); exchange.close(); });
        server.start();
        try {
            var client = new MockValhallaClient("http://127.0.0.1:" + server.getAddress().getPort());
            var routes = client.route(new RoutePlanRequest(RoutingTestFixtures.FROM,
                    RoutingTestFixtures.TO, RoutingTestFixtures.VEHICLE, List.of(), 3));
            assertThat(routes).hasSize(1);
            assertThat(routes.get(0).explanation()).contains("폴백");
        } finally { server.stop(0); }
    }

    // OSRM 이 "이 좌표 사이에 길이 없다"고 답한 것과 OSRM 을 못 불렀다는 것은 다른 상황이다.
    // 전자는 정상 응답(빈 후보), 후자는 502 여야 프론트가 라우터 장애를 진입 불가로 안 그린다.
    @Test
    void no_route_answer_is_not_the_same_as_a_broken_router() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/route", exchange -> {
            byte[] body = "{\"code\":\"NoRoute\",\"routes\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            var client = new MockValhallaClient("http://127.0.0.1:" + server.getAddress().getPort());
            // 라우터는 멀쩡히 답했다. 후보가 없을 뿐이라 예외가 아니라 빈 목록이다.
            assertThat(client.route(new RoutePlanRequest(RoutingTestFixtures.FROM,
                    RoutingTestFixtures.TO, RoutingTestFixtures.VEHICLE, List.of(), 3))).isEmpty();
        } finally { server.stop(0); }
    }
}
