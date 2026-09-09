package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.*;
import com.fireway.backend.modules.routing.application.port.RoutePlanRequest;
import com.fireway.backend.modules.routing.infrastructure.RealValhallaClient;
import com.fireway.backend.shared.exception.ExternalSystemException;
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

    @Test
    void translates_remote_http_errors_to_external_system_errors() {
        assertThatThrownBy(() -> client(HttpStatus.SERVICE_UNAVAILABLE, "{}").route(request()))
                .isInstanceOf(ExternalSystemException.class);
    }

    @Test
    void rejects_malformed_trip_geometry() {
        assertThatThrownBy(() -> client(HttpStatus.OK, """
                {"trip":{"status":0,"summary":{"time":200,"length":1},"legs":[{"shape":"?"}]}}
                """).route(request())).isInstanceOf(ExternalSystemException.class);
    }
}
