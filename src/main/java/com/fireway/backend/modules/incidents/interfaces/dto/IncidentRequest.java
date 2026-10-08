package com.fireway.backend.modules.incidents.interfaces.dto;
import com.fireway.backend.modules.incidents.domain.Incident;
import jakarta.validation.constraints.*;
/** 신고 접수 요청. 필드는 snake_case 로 들어온다(JacksonConfig). address·lat·lon 외에는 전부 선택. */
public record IncidentRequest(
        @NotBlank @Size(max = 200) String address,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double lon,
        @Size(max = 500) String summary,
        @Size(max = 50) String reporterName,
        @Size(max = 20) String reporterPhone,
        @Pattern(regexp = "small|medium|large") String severity,
        @PositiveOrZero Integer estimatedAreaM2,
        @Size(max = 100) String buildingType,
        Boolean casualtiesReported,
        @Size(max = 500) String notes) {
    public Incident.Intake intake() {
        return new Incident.Intake(reporterName, reporterPhone, severity, estimatedAreaM2, buildingType,
                Boolean.TRUE.equals(casualtiesReported), notes);
    }
}
