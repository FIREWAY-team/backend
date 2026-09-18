package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.*;
import com.fireway.backend.modules.routing.application.port.RoutePlanRequest;
import com.fireway.backend.modules.routing.infrastructure.RealValhallaClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

class RealValhallaClientTest {
    private RoutePlanRequest request() {
        return new RoutePlanRequest(RoutingTestFixtures.FROM, RoutingTestFixtures.TO,
                RoutingTestFixtures.VEHICLE, List.of(), 3);
    }

    private RealValhallaClient client(HttpStatus status, String body) {
        return new RealValhallaClient(WebClient.builder().exchangeFunction(request -> {
            assertThat(request.method()).isEqualTo(HttpMethod.POST);
            assertThat(request.url().toString()).isEqualTo("http://valhalla.test/route");
            return Mono.just(ClientResponse.create(status).header("Content-Type", "application/json")
                    .body(body).build());
        }), "http://valhalla.test");
    }

    @Test
    void parses_primary_and_alternate_trips_using_polyline6_and_meters() {
        // Independent known polyline6 encoding of [0,0] -> [0.001,0.001].
        var result = client(HttpStatus.OK, """
                {"trip":{"status":0,"summary":{"time":200.2,"length":1.25},"legs":[{"shape":"??o}@o}@"}]},
                 "alternates":[{"trip":{"status":0,"summary":{"time":280,"length":1.5},
                 "legs":[{"shape":"??o}@o}@"}]}}]}
                """).route(request());
        assertThat(result).hasSize(2);
        assertThat(result.get(0).etaSec()).isEqualTo(201);
        assertThat(result.get(0).distanceM()).isEqualTo(1250);
        assertThat(result.get(0).coordinates().get(1)).containsExactly(0.001, 0.001);
        assertThat(result.get(1).rank()).isEqualTo(2);
        assertThat(result.get(1).etaSec()).isEqualTo(280);
    }

    // ponytail: 시연 마감 폴백 도입 후 원격 실패·응답 오류는 예외 대신 직선 폴백 1개를 반환한다.
    // (§RealValhallaClient · fallbackStraightLine). Valhalla 안정화되면 원래 계약으로 복귀한다.
    @Test
    void remote_http_errors_return_demo_fallback_line() {
        var routes = client(HttpStatus.SERVICE_UNAVAILABLE, "{}").route(request());
        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).explanation()).contains("폴백");
    }

    @Test
    void malformed_trip_returns_demo_fallback_line() {
        var routes = client(HttpStatus.OK, """
                {"trip":{"status":0,"summary":{"time":200,"length":1},"legs":[{"shape":"?"}]}}
                """).route(request());
        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).explanation()).contains("폴백");
    }
}
