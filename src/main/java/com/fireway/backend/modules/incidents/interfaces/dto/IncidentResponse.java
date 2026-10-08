package com.fireway.backend.modules.incidents.interfaces.dto;
import com.fireway.backend.modules.incidents.domain.Incident;
import java.time.LocalDateTime;
public record IncidentResponse(String incidentNo, String status, String address,
                               double lat, double lon, String summary,
                               LocalDateTime receivedAt, LocalDateTime closedAt,
                               String reporterName, String reporterPhone, String severity,
                               Integer estimatedAreaM2, String buildingType, boolean casualtiesReported,
                               String notes) {
    public static IncidentResponse from(Incident i) {
        Incident.Intake in = i.intake();
        return new IncidentResponse(i.incidentNo(), i.status().name(), i.address(),
                i.lat(), i.lon(), i.summary(), i.receivedAt(), i.closedAt(),
                in.reporterName(), in.reporterPhone(), in.severity(), in.estimatedAreaM2(),
                in.buildingType(), in.casualtiesReported(), in.notes());
    }
}
