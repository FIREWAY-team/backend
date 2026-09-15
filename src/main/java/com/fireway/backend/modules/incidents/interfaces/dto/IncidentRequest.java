package com.fireway.backend.modules.incidents.interfaces.dto;
import jakarta.validation.constraints.*;
/** 신고 접수 요청. 필드는 snake_case 로 들어온다(JacksonConfig). */
public record IncidentRequest(
        @NotBlank @Size(max = 200) String address,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double lon,
        @Size(max = 500) String summary) { }
