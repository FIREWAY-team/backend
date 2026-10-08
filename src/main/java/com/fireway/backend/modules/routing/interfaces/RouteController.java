package com.fireway.backend.modules.routing.interfaces;

import com.fireway.backend.modules.routing.application.RoutePlanner;
import com.fireway.backend.modules.routing.interfaces.dto.*;
import com.fireway.backend.shared.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/route")
public class RouteController {
    // 차량별 계산은 대부분 OSRM 응답을 기다리는 시간이라 스레드 몇 개로 충분하다.
    // commonPool 은 2코어 장비에서 병렬도가 1이라 쓰지 않는다(MockValhallaClient 주석 참고).
    private static final ExecutorService VEHICLE_POOL = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "route-vehicle");
        thread.setDaemon(true);
        return thread;
    });

    private final RoutePlanner planner;

    public RouteController(RoutePlanner planner) { this.planner = planner; }

    @PostMapping
    public RouteResponse route(@Valid @RequestBody RouteRequest request) {
        return RouteResponse.from(planner.plan(request.toCommand()));
    }

    /**
     * 차량 여러 대를 한 번에. 차량마다 CCTV 판독이 달라 막히는 골목이 다르므로 경로도 따로 나온다.
     * 병렬로 돌기 때문에 차량 1대일 때와 걸리는 시간이 비슷하다. 응답 키는 요청한 vehicle_id.
     */
    @PostMapping("/batch")
    public RouteBatchResponse batch(@Valid @RequestBody RouteBatchRequest request) {
        Map<String, CompletableFuture<RouteResponse>> pending = new LinkedHashMap<>();
        for (String vehicleId : new LinkedHashSet<>(request.vehicleIds())) {
            pending.put(vehicleId, CompletableFuture.supplyAsync(
                    () -> RouteResponse.from(planner.plan(request.toCommand(vehicleId))), VEHICLE_POOL));
        }
        Map<String, RouteResponse> routes = new LinkedHashMap<>();
        try {
            for (var entry : pending.entrySet()) routes.put(entry.getKey(), entry.getValue().join());
        } catch (CompletionException error) {
            // 없는 차량(404) 같은 도메인 예외를 감싼 채로 올리면 500 이 된다. 원래 예외로 푼다.
            if (error.getCause() instanceof RuntimeException cause) throw cause;
            throw error;
        }
        return new RouteBatchResponse(routes);
    }

    public record RouteBatchResponse(@com.fasterxml.jackson.annotation.JsonProperty("routes_by_vehicle")
                                     Map<String, RouteResponse> routesByVehicle) { }

    // This API uses 400; other modules retain the shared validation contract (422).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(new ErrorResponse.ErrorBody(
                "VALIDATION_ERROR", "요청 값이 유효하지 않습니다.", request.getHeader("X-Request-Id"))));
    }
}
