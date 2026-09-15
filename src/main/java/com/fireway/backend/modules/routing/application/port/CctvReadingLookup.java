package com.fireway.backend.modules.routing.application.port;

import com.fireway.backend.modules.routing.domain.CctvVerdict;
import java.util.*;

/**
 * Inbound port for consulting the AI pipeline's per-frame verdicts from inside the routing planner.
 * <p>
 * The routing 3-layer decision (§FRONTEND_SPEC §5-3): a static no-go polygon (layer 1, from the PDF
 * baseline) can be overridden by a real-time CCTV verdict (layer 3) if the pipeline judged the current
 * vehicle as {@code PASS}. This port is how the {@code RoutePlanner} asks "does any recent CCTV
 * verdict cover this specific no-go polygon, and does it clear my vehicle?" — the answer decides
 * whether that no-go blocks the route or is unlocked.
 * <p>
 * ⚠️ Empty result ≠ pass. When no verdict exists for a polygon, we assume the static no-go still
 * stands — no evidence to unlock it.
 * ⚠️ Query is per polygon id (as issued by the staticdata module), not per road link. Precise
 * edge-level matching arrives with the Valhalla tile switch (§routing PR #11).
 */
public interface CctvReadingLookup {

    /**
     * Returns the most recent CCTV verdict associated with the given no-go polygon, or empty if none.
     *
     * @param polygonId identifier from {@link com.fireway.backend.modules.routing.domain.NoGoAreaSummary#polygonId()}
     */
    Optional<CctvVerdict> forPolygon(String polygonId);

    default Map<String, Optional<CctvVerdict>> forPolygons(List<String> ids) {
        Map<String, Optional<CctvVerdict>> result = new HashMap<>();
        for (String id : ids) result.put(id, forPolygon(id));
        return result;
    }
}
