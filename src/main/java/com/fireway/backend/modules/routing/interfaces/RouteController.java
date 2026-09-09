package com.fireway.backend.modules.routing.interfaces;

import com.fireway.backend.modules.routing.application.RoutePlanner;
import com.fireway.backend.modules.routing.interfaces.dto.*;
import com.fireway.backend.shared.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/route")
public class RouteController {
    private final RoutePlanner planner;

    public RouteController(RoutePlanner planner) { this.planner = planner; }

    @PostMapping
    public RouteResponse route(@Valid @RequestBody RouteRequest request) {
        return RouteResponse.from(planner.plan(request.toCommand()));
    }

    // This API uses 400; other modules retain the shared validation contract (422).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(new ErrorResponse.ErrorBody(
                "VALIDATION_ERROR", "요청 값이 유효하지 않습니다.", request.getHeader("X-Request-Id"))));
    }
}
