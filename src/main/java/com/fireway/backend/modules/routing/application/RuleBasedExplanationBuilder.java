package com.fireway.backend.modules.routing.application;

import com.fireway.backend.modules.routing.domain.RouteCandidate;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Human-readable rationale for one route candidate — used by the situation-room banner and evidence
 * sheet. Kept as a rule-based composer (no LLM) so the explanation is deterministic and testable, and
 * the LLM stub can slot in later behind the same {@link ExplanationBuilder} port.
 * <p>
 * The rationale walks the same 3-layer decision the planner already made: golden time, static no-go
 * survivors after the CCTV overlay, and how many CCTV unlocks made this route viable in the first
 * place.
 */
@Component
public class RuleBasedExplanationBuilder implements ExplanationBuilder {
    @Override
    public String build(RouteCandidate candidate, Vehicle vehicle, int goldenTimeSec) {
        String within = goldenTimeSec == 300 ? "골든타임 5분 이내" : "골든타임 " + goldenTimeSec + "초 이내";
        String timeSummary = candidate.meetsGoldenTime() ? within : "골든타임 초과";
        int unlocked = candidate.unlockedByCctv().size();
        int stillBlocked = candidate.excludedReasons().size();
        String cctvSummary;
        if (!candidate.passableForVehicle()) {
            cctvSummary = String.format(Locale.ROOT,
                    "차량 폭(%sm) 기준 통행 불가 또는 CCTV 미확인 골목 %d건 — 진입 경로로 추천하지 않습니다.",
                    vehicle.widthM(), stillBlocked);
        } else if (unlocked > 0) {
            cctvSummary = String.format(Locale.ROOT,
                    "CCTV 판독으로 정적 진입곤란 %d건 해제(잔여폭 %sm 통과). 미해결 %d건.",
                    unlocked, vehicle.widthM(), stillBlocked);
        } else if (stillBlocked > 0) {
            cctvSummary = String.format(Locale.ROOT,
                    "CCTV 판독 없음. 정적 진입곤란 %d건이 우회 근거로 남음.", stillBlocked);
        } else {
            cctvSummary = "정적 진입곤란 구간 없음.";
        }
        return String.format(Locale.ROOT,
                "%d순위 후보 · 예상 %d초 (%s) · 통과확률 %.0f%% · 차량 %s(폭 %sm). %s",
                candidate.rank(), candidate.etaSec(), timeSummary, candidate.passableProb() * 100,
                vehicle.name(), vehicle.widthM(), cctvSummary);
    }
}
