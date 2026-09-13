package com.fireway.backend.modules.staticdata.interfaces;
import com.fireway.backend.modules.staticdata.application.NoGoAreaService;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/no_go") public class NoGoController {
    private final NoGoAreaService service;
    public NoGoController(NoGoAreaService service) { this.service = service; }

    /** 지도 표시용이라 unverified 도 내려간다. 프론트는 verification_status 로 구분한다. */
    @GetMapping public List<NoGoResponse> list() {
        return service.findAll().stream().map(NoGoController::toResponse).toList();
    }

    private static NoGoResponse toResponse(NoGoArea area) {
        // GeoJSON 표준과 같은 [lon, lat] 순서다. 원본 GeoJSON·DB WKT·임포트 스크립트가 모두 이 순서를 쓴다.
        List<List<Double>> path = area.path().stream().map(c -> List.of(c.lon(), c.lat())).toList();
        return new NoGoResponse(area.id(), area.extId(), area.dong(), area.reason(), area.layer(),
                area.verificationStatus(), area.note(), area.geometryType().geoJsonName(), path);
    }

    public record NoGoResponse(long id, String extId, String dong, String reason, int layer,
                               String verificationStatus, String note, String geometryType,
                               List<List<Double>> path) { }
}
