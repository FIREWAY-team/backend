package com.fireway.backend.modules.routing.application;

import com.fireway.backend.modules.routing.domain.RouteCandidate;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.List;

/**
 * 라우팅 계산 결과 애그리게이트.
 * - noGoConsidered: 계산 시점에 로드된 활성 no-go 폴리곤 수 (전체, 경로별이 아님).
 * - valhallaMocked / noGoMocked: 응답 신뢰도 전달용 — 프론트/심사 투명성.
 * - vehicleUsed: 응답에 실 사용 차량 스펙 포함 (프론트가 별도 조회 안 하도록).
 */
public record RoutePlanResult(List<RouteCandidate> routes, long calcTimeMs, int kEffective,
                              double[][] overlapMatrix, String alternativesStatus,
                              int noGoConsidered, boolean valhallaMocked, boolean noGoMocked,
                              Vehicle vehicleUsed) { }
