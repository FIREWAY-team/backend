package com.fireway.backend.modules.routing.domain;

import java.util.Map;

/**
 * Routing-owned projection of a CCTV frame verdict.
 * <p>
 * The AI pipeline (§유강현 ai repo · {@code src/pipeline.py} · {@code ReadingCore}) produces per-vehicle
 * verdicts for each CCTV frame: for each vehicle id (e.g. {@code pump-3.5}, {@code pump-8}) it emits
 * either {@code "PASS"} or {@code "FAIL"} based on the frame's measured lane width vs the vehicle width
 * plus a safety margin. This record is the routing module's inbound-only view of that verdict — the
 * cctv module owns persistence/ingest, routing just consults it to decide whether a static no-go can be
 * unlocked for the currently dispatched vehicle.
 * <p>
 * ⚠️ Missing verdict for a vehicle id ≠ pass. If the AI pipeline did not judge that vehicle for this
 * frame (rare — pipeline computes all registered vehicles at once), treat as {@code FAIL} — no
 * evidence, no unlock.
 *
 * @param cctvId identifier of the CCTV frame the verdict was computed from
 * @param effectiveWidthM measured lane width remaining after obstacles (meters, from {@code ReadingCore.effective_width_m})
 * @param verdictByVehicle vehicle id → {@code "PASS"} or {@code "FAIL"} (or {@code "UNCERTAIN"} — treat as FAIL for routing)
 * @param confidence pipeline confidence (0..1), the most conservative per-vehicle judgment probability
 */
public record CctvVerdict(String cctvId, double effectiveWidthM,
                          Map<String, String> verdictByVehicle, double confidence) {

    /** Whether this CCTV verdict passes the given vehicle id. Anything other than {@code "PASS"} is refused. */
    public boolean passes(String vehicleId) {
        return "PASS".equalsIgnoreCase(verdictByVehicle.get(vehicleId));
    }
}
