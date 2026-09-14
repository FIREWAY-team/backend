package com.fireway.backend.modules.routing.infrastructure;

import com.fireway.backend.modules.routing.application.port.CctvReadingLookup;
import com.fireway.backend.modules.routing.domain.CctvVerdict;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Deterministic mock CCTV verdicts for the routing planner. Every no-go polygon id gets a
 * reproducible verdict derived from a hash of its id, so the same simulated frame comes back on
 * every request — the demo does not flicker between reloads.
 * <p>
 * Contract with the real (future) adapter: same port, same shape, no code changes needed in
 * {@code RoutePlanner} when the AI pipeline's persisted verdicts replace this. See §goldenlane-project
 * for the ingest path plan.
 * <p>
 * ⚠️ **This is a stand-in for demo/시연 while the AI pipeline's persisted output is not yet wired
 * into the backend cctv module.** The routing 3-layer decision (static no-go × CCTV verdict × vehicle)
 * is exercised end-to-end with these verdicts.
 * ⚠️ Roughly 45% of polygons yield a PASS verdict (bucket 0..44) for small pumper (pump-3.5), 25% also
 * pass mid pumper (pump-8), 10% also pass large pumper (pump-15). The rest fail every vehicle.
 */
@Component
public class MockCctvReadingLookup implements CctvReadingLookup {

    @Override
    public Optional<CctvVerdict> forPolygon(String polygonId) {
        if (polygonId == null || polygonId.isBlank()) return Optional.empty();
        int bucket = Math.floorMod(polygonId.hashCode(), 100);
        // 나머지 55개 버킷은 verdict 자체가 없는 것으로 취급 — CCTV 미배치 골목이 대부분이라는 현실을 반영.
        if (bucket >= 45) return Optional.empty();

        // effective width: 판정 강도와 상관을 보여주기 위해 bucket 이 낮을수록 넓게 잡는다.
        double effectiveWidthM = 2.0 + (44 - bucket) * 0.04;

        String smallVerdict = "PASS"; // bucket < 45 는 모두 소형 통과
        String midVerdict = bucket < 25 ? "PASS" : "FAIL";
        String largeVerdict = bucket < 10 ? "PASS" : "FAIL";
        double confidence = 0.70 + (44 - bucket) * 0.005;

        return Optional.of(new CctvVerdict(
                "cctv-" + Integer.toHexString(polygonId.hashCode() & 0xFFFF),
                effectiveWidthM,
                Map.of("pump-3.5", smallVerdict, "pump-8", midVerdict, "pump-15", largeVerdict),
                Math.min(0.98, confidence)));
    }
}
