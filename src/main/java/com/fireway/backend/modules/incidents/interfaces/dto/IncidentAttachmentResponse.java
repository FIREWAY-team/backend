package com.fireway.backend.modules.incidents.interfaces.dto;
import com.fireway.backend.modules.incidents.domain.IncidentAttachment;
import java.time.LocalDateTime;
public record IncidentAttachmentResponse(String key, String contentType, long sizeBytes,
                                         String downloadUrl, LocalDateTime createdAt) {
    public static IncidentAttachmentResponse from(IncidentAttachment a, String downloadUrl) {
        return new IncidentAttachmentResponse(a.fileKey(), a.contentType(), a.sizeBytes(),
                downloadUrl, a.createdAt());
    }
}
