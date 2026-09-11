package com.fireway.backend.modules.routing.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fireway.backend.modules.routing.application.RoutePlanResult;
import java.util.ArrayList;
import java.util.List;

public record RouteResponse(List<RouteCandidateDto> routes,
        @JsonProperty("calc_time_ms") long calcTimeMs,
        @JsonProperty("k_effective") int kEffective,
        @JsonProperty("overlap_matrix") double[][] overlapMatrix,
        @JsonProperty("alternatives_status") String alternativesStatus,
        @JsonProperty("no_go_considered") int noGoConsidered,
        @JsonProperty("vehicle_used") VehicleUsedDto vehicleUsed,
        List<String> warnings) {
    public static RouteResponse from(RoutePlanResult result) {
        List<String> warnings = new ArrayList<>();
        if (result.valhallaMocked())
            warnings.add("valhalla_mock: 실 Valhalla tile 대신 mock 후보를 반환했습니다.");
        if (result.noGoMocked())
            warnings.add("no_go_mock: 이태연 staticdata 브랜치 머지 전 임시 폴리곤을 사용했습니다.");
        if ("partial".equals(result.alternativesStatus()))
            warnings.add("no_route_within_golden_time: 5분 이내 도달 후보가 없습니다.");
        if ("no_alternative".equals(result.alternativesStatus()))
            warnings.add("no_alternative: 유효 후보가 없습니다. 대로 정차·호스 전개를 검토하세요.");
        return new RouteResponse(
                result.routes().stream().map(RouteCandidateDto::from).toList(),
                result.calcTimeMs(), result.kEffective(), result.overlapMatrix(), result.alternativesStatus(),
                result.noGoConsidered(), VehicleUsedDto.from(result.vehicleUsed()), warnings);
    }

    public record VehicleUsedDto(String id, String name,
            @JsonProperty("width_m") double widthM,
            @JsonProperty("height_m") double heightM,
            @JsonProperty("length_m") double lengthM,
            @JsonProperty("weight_ton") double weightTon,
            @JsonProperty("turning_radius_m") double turningRadiusM) {
        static VehicleUsedDto from(com.fireway.backend.modules.vehicles.domain.Vehicle v) {
            return new VehicleUsedDto(v.vehicleId(), v.name(), v.widthM(), v.heightM(), v.lengthM(),
                    v.weightTon(), v.turningRadiusM());
        }
    }
}
