package com.fireway.backend.modules.incidents.application.port;
import com.fireway.backend.modules.incidents.domain.IncidentAttachment;
import java.util.List;
public interface IncidentAttachmentRepository {
    /** 저장하고 부여된 PK 를 돌려준다. file_key 가 이미 붙어 있으면 DuplicateKeyException 이 올라온다. */
    long insert(IncidentAttachment attachment);

    /** 붙인 순서대로. */
    List<IncidentAttachment> findByIncidentId(long incidentId);
}
