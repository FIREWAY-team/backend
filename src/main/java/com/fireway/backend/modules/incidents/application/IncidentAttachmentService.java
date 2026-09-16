package com.fireway.backend.modules.incidents.application;
import com.fireway.backend.modules.incidents.application.port.IncidentAttachmentRepository;
import com.fireway.backend.modules.incidents.domain.Incident;
import com.fireway.backend.modules.incidents.domain.IncidentAttachment;
import com.fireway.backend.shared.exception.ConflictException;
import com.fireway.backend.shared.storage.FileUploadService;
import com.fireway.backend.shared.storage.StoragePort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 업로드 3단계 중 마지막 — 프론트가 S3 에 직접 올린 파일의 key 를 신고에 붙인다.
 *
 * 프론트의 "올렸어요" 보고를 그대로 믿지 않는다. confirmUpload() 가 S3 에 실제로 있는지,
 * 크기 상한(사진 5MB · 동영상 50MB)을 넘지 않는지 확인한 뒤에만 저장한다.
 */
@Service public class IncidentAttachmentService {
    private final IncidentService incidents;
    private final FileUploadService files;
    private final IncidentAttachmentRepository repository;
    private final Clock clock;

    public IncidentAttachmentService(IncidentService incidents, FileUploadService files,
                                     IncidentAttachmentRepository repository, Clock clock) {
        this.incidents = incidents;
        this.files = files;
        this.repository = repository;
        this.clock = clock;
    }

    public IncidentAttachment attach(String incidentNo, String key) {
        Incident incident = incidents.get(incidentNo);           // 없으면 여기서 404. S3 는 건드리지 않는다
        StoragePort.StoredObject object = files.confirmUpload(key);   // 없거나 크면 422 (큰 파일은 지워진다)
        IncidentAttachment draft = IncidentAttachment.attached(incident.id(), object.key(),
                object.contentType(), object.sizeBytes(), LocalDateTime.now(clock));
        try {
            return draft.withId(repository.insert(draft));
        } catch (DuplicateKeyException e) {
            throw new ConflictException("이미 첨부된 파일입니다: " + key);
        }
    }

    public List<IncidentAttachment> findFor(String incidentNo) {
        return repository.findByIncidentId(incidents.get(incidentNo).id());
    }

    /** 조회 URL 은 저장하지 않고 내려줄 때마다 새로 서명한다(몇 분이면 만료된다). */
    public String downloadUrl(IncidentAttachment attachment) {
        return files.issueDownloadUrl(attachment.fileKey());
    }
}
