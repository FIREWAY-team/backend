package com.fireway.backend.modules.incidents.domain;
import java.time.LocalDateTime;
import java.util.List;
/**
 * 신고 한 건에 대해 산출한 경로 후보 하나와 그 판단 근거.
 *
 * 라우팅은 결과를 응답으로 한 번 뱉고 끝이라 "왜 이 경로였는지" 가 휘발된다.
 * 안전 제품에서 그건 나중에 설명할 방법이 없다는 뜻이라 여기 남긴다.
 */
public record IncidentRoute(long id, long incidentId, String vehicleId, int rank,
                            Integer distanceM, Integer etaSeconds, String polyline,
                            boolean passableForVehicle, boolean meetsGoldenTime, Double passableProb,
                            String explanation, List<Blocked> excludedReasons,
                            List<String> unlockedByCctv, LocalDateTime plannedAt) {
    /** 이 경로에서 끝내 막힌 구간. polygonId 는 no_go_areas.ext_id 다. */
    public record Blocked(String polygonId, String reason, String evidenceUrl) { }

    /** 권장 경로인지. 순위 1이면서 차량이 실제로 지날 수 있어야 한다. */
    public boolean recommended() { return rank == 1 && passableForVehicle; }
}
