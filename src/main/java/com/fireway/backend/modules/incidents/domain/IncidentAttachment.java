package com.fireway.backend.modules.incidents.domain;
import java.time.LocalDateTime;
/**
 * 신고에 붙은 사진·동영상 한 건. 파일은 S3 에 있고 여기엔 key 만 둔다.
 * 조회 URL 은 저장하지 않고 내려줄 때마다 새로 서명한다.
 */
public record IncidentAttachment(long id, long incidentId, String fileKey, String contentType,
                                 long sizeBytes, LocalDateTime createdAt) {

    /** 첨부 직후 상태. id 는 저장 시점에 채워진다. */
    public static IncidentAttachment attached(long incidentId, String fileKey, String contentType,
                                              long sizeBytes, LocalDateTime createdAt) {
        return new IncidentAttachment(0, incidentId, fileKey, contentType, sizeBytes, createdAt);
    }

    public IncidentAttachment withId(long assigned) {
        return new IncidentAttachment(assigned, incidentId, fileKey, contentType, sizeBytes, createdAt);
    }
}
