package com.fireway.backend.modules.routing;

import com.fireway.backend.modules.routing.application.*;
import com.fireway.backend.modules.routing.domain.*;
import com.fireway.backend.modules.routing.infrastructure.PolylineCodec;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.ArrayList;
import java.util.List;

final class RoutingTestFixtures {
    static final Coordinate FROM = new Coordinate(37.44, 127.14);
    static final Coordinate TO = new Coordinate(37.45, 127.16);
    static final Vehicle VEHICLE = new Vehicle("pump-3.5", "소형펌프차", 2.3, 3.0, 7.0, 3.5, 6.5);
    static RoutePlanningCommand command() { return new RoutePlanningCommand(VEHICLE.vehicleId(), FROM, TO, 3, 0.65, 300); }
    /**
     * 결정론적 3후보 — 옛 MockValhallaClient 의 직선 폴백과 동일 shape. OSRM 네트워크 콜을
     * 테스트에서 배제하려고 여기 인라인. (실제 배포 라우터는 project-osrm 이나 Valhalla.)
     */
    static List<RouteCandidate> candidates() {
        double lon = FROM.lon(), lat = FROM.lat();
        double dx = TO.lon() - lon, dy = TO.lat() - lat;
        double distance = 111_320 * Math.hypot(dx * Math.cos(Math.toRadians((lat + TO.lat()) / 2)), dy);
        List<RouteCandidate> out = new ArrayList<>();
        int[] etas = {200, 280, 340};
        for (int i = 0; i < 3; i++) {
            double jitter = (i - 1) * 0.12;
            List<double[]> points = List.of(new double[]{lon, lat},
                    new double[]{lon + dx * 0.5 - dy * jitter, lat + dy * 0.5 + dx * jitter},
                    new double[]{TO.lon(), TO.lat()});
            out.add(new RouteCandidate(i + 1, points, PolylineCodec.encode(points), etas[i], distance * (1 + i * 0.15)));
        }
        return out;
    }
    static RouteCandidate candidate(int rank, int eta, double[]... points) {
        return new RouteCandidate(rank, List.of(points), "", eta, 1000);
    }
}
