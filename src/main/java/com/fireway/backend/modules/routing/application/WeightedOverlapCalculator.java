package com.fireway.backend.modules.routing.application;

import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class WeightedOverlapCalculator {
    public double overlap(RouteCandidate a, RouteCandidate b) {
        Map<String, Double> segsA = segmentLengths(a.coordinates());
        Map<String, Double> segsB = segmentLengths(b.coordinates());
        double shared = segsA.entrySet().stream().filter(e -> segsB.containsKey(e.getKey()))
                .mapToDouble(e -> Math.min(e.getValue(), segsB.get(e.getKey()))).sum();
        Set<String> union = new HashSet<>(segsA.keySet());
        union.addAll(segsB.keySet());
        double total = union.stream().mapToDouble(k -> Math.max(segsA.getOrDefault(k, 0.0), segsB.getOrDefault(k, 0.0))).sum();
        return total > 0 ? shared / total : 0.0;
    }

    public List<RouteCandidate> filter(List<RouteCandidate> candidates, double threshold) {
        List<RouteCandidate> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator.comparingInt(RouteCandidate::rank));
        List<RouteCandidate> accepted = new ArrayList<>();
        for (RouteCandidate candidate : ordered) {
            if (accepted.stream().noneMatch(previous -> overlap(previous, candidate) > threshold)) {
                accepted.add(candidate);
            }
        }
        return accepted;
    }

    public double[][] matrix(List<RouteCandidate> routes) {
        double[][] matrix = new double[routes.size()][routes.size()];
        for (int i = 0; i < routes.size(); i++) {
            for (int j = i; j < routes.size(); j++) {
                matrix[i][j] = matrix[j][i] = overlap(routes.get(i), routes.get(j));
            }
        }
        return matrix;
    }

    private Map<String, Double> segmentLengths(List<double[]> coordinates) {
        Map<String, Double> segments = new HashMap<>();
        for (int i = 1; i < coordinates.size(); i++) {
            double[] a = coordinates.get(i - 1), b = coordinates.get(i);
            // Direction-invariant key: 같은 도로 세그먼트를 반대 방향으로 지나가도 같은 키로 인식되게.
            String key = normalizeSegmentKey(a, b);
            double lat = Math.toRadians((a[1] + b[1]) / 2);
            double length = 111_320 * Math.hypot((b[0] - a[0]) * Math.cos(lat), b[1] - a[1]);
            segments.merge(key, length, Double::sum);
        }
        return segments;
    }

    private String normalizeSegmentKey(double[] a, double[] b) {
        String endA = String.format(Locale.ROOT, "%.5f,%.5f", a[0], a[1]);
        String endB = String.format(Locale.ROOT, "%.5f,%.5f", b[0], b[1]);
        return endA.compareTo(endB) <= 0 ? endA + "|" + endB : endB + "|" + endA;
    }
}
