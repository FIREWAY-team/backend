package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import com.fireway.backend.modules.routing.application.port.RoutePlanRequest;
import com.fireway.backend.modules.routing.domain.NoGoAreaSummary;
import com.fireway.backend.modules.routing.infrastructure.MockValhallaClient;
import com.fireway.backend.shared.exception.ExternalSystemException;
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

    @Test
    void unavailable_router_does_not_return_straight_line_routes() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/route", exchange -> { exchange.sendResponseHeaders(503, -1); exchange.close(); });
        server.start();
        try {
            var client = new MockValhallaClient("http://127.0.0.1:" + server.getAddress().getPort());
            assertThatThrownBy(() -> client.route(new RoutePlanRequest(RoutingTestFixtures.FROM,
                    RoutingTestFixtures.TO, RoutingTestFixtures.VEHICLE, List.of(), 3)))
                    .isInstanceOf(ExternalSystemException.class);
        } finally { server.stop(0); }
    }
}
