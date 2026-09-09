package com.fireway.backend.modules.routing.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fireway.backend.modules.routing.domain.ExcludedReason;

public record ExcludedReasonDto(@JsonProperty("polygon_id") String polygonId, String reason,
                                @JsonProperty("evidence_url") String evidenceUrl) {
    public static ExcludedReasonDto from(ExcludedReason reason) {
        return new ExcludedReasonDto(reason.polygonId(), reason.reason(), reason.evidenceUrl());
    }
}
