package com.fireway.backend.modules.incidents.interfaces;
import com.fireway.backend.modules.incidents.application.IncidentAttachmentService;
import com.fireway.backend.modules.incidents.interfaces.dto.AttachmentRequest;
import com.fireway.backend.modules.incidents.interfaces.dto.IncidentAttachmentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/incidents/{incidentNo}/attachments")
public class IncidentAttachmentController {
    private final IncidentAttachmentService service;
    public IncidentAttachmentController(IncidentAttachmentService service) { this.service = service; }

    /** POST /api/files/upload-url 로 받은 key 를, S3 에 PUT 을 끝낸 뒤 보낸다. */
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public IncidentAttachmentResponse attach(@PathVariable String incidentNo,
                                             @Valid @RequestBody AttachmentRequest request) {
        var attached = service.attach(incidentNo, request.key());
        return IncidentAttachmentResponse.from(attached, service.downloadUrl(attached));
    }

    /** 붙인 순서대로. 아직 없으면 빈 목록. download_url 은 부를 때마다 새로 서명된다. */
    @GetMapping public List<IncidentAttachmentResponse> list(@PathVariable String incidentNo) {
        return service.findFor(incidentNo).stream()
                .map(a -> IncidentAttachmentResponse.from(a, service.downloadUrl(a)))
                .toList();
    }
}
