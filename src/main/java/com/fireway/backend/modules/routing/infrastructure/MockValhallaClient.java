package com.fireway.backend.modules.routing.infrastructure;

import com.fireway.backend.modules.routing.application.port.*;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
@Primary
public class MockValhallaClient implements ValhallaClient {
    @Override
    public List<RouteCandidate> route(RoutePlanRequest request) {
        double lon = request.from().lon(), lat = request.from().lat();
        double dx = request.to().lon() - lon, dy = request.to().lat() - lat;
        double distance = 111_320 * Math.hypot(dx * Math.cos(Math.toRadians((lat + request.to().lat()) / 2)), dy);
        List<RouteCandidate> candidates = new ArrayList<>();
        int[] etas = {200, 280, 340};
        for (int i = 0; i < 3; i++) {
            double jitter = (i - 1) * 0.12;
            List<double[]> points = List.of(new double[]{lon, lat},
                    new double[]{lon + dx * 0.5 - dy * jitter, lat + dy * 0.5 + dx * jitter},
                    new double[]{request.to().lon(), request.to().lat()});
            candidates.add(new RouteCandidate(i + 1, points, PolylineCodec.encode(points), etas[i], distance * (1 + i * 0.15)));
        }
        return candidates;
    }
}
