package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.*;
import com.fireway.backend.modules.routing.application.WeightedOverlapCalculator;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeightedOverlapCalculatorTest {
    private final WeightedOverlapCalculator calculator = new WeightedOverlapCalculator();
    private final double[] a = {127.00, 37.0}, b = {127.01, 37.0}, c = {127.02, 37.0}, d = {127.03, 37.0};
    @Test
    void overlap_of_identical_routes_is_1() {
        assertThat(calculator.overlap(RoutingTestFixtures.candidate(1, 200, a, b, c),
                RoutingTestFixtures.candidate(2, 280, a, b, c))).isEqualTo(1.0);
    }
    @Test
    void overlap_of_disjoint_routes_is_0() {
        assertThat(calculator.overlap(RoutingTestFixtures.candidate(1, 200, a, b),
                RoutingTestFixtures.candidate(2, 280, c, d))).isZero();
    }
    @Test
    void overlap_of_half_shared_is_around_half() {
        // The denominator is the union: one shared segment out of two distinct segments.
        assertThat(calculator.overlap(RoutingTestFixtures.candidate(1, 200, a, b),
                RoutingTestFixtures.candidate(2, 280, a, b, c))).isCloseTo(0.5, within(0.00001));
    }
    @Test
    void filter_removes_high_overlap_lower_rank() {
        var first = RoutingTestFixtures.candidate(1, 200, a, b);
        var duplicate = RoutingTestFixtures.candidate(2, 280, a, b);
        var disjoint = RoutingTestFixtures.candidate(3, 340, c, d);
        assertThat(calculator.filter(List.of(duplicate, disjoint, first), 0.65)).containsExactly(first, disjoint);
        assertThat(calculator.filter(List.of(first, duplicate), 1.0)).containsExactly(first, duplicate);
    }
}
