package com.fireway.backend.modules.staticdata.interfaces;
import com.fireway.backend.modules.staticdata.application.NoGoAreaService;
import com.fireway.backend.modules.staticdata.domain.BoundingBox;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import com.fireway.backend.shared.exception.ValidationException;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/no_go") public class NoGoController {
    // 관내도에서 뽑은 정적 데이터다. 재임포트 전에는 바뀌지 않으므로 캐시해도 안전하다.
    // 지도를 움직일 때마다 같은 응답을 다시 받는 것을 막는 게 목적이라 5분이면 충분하다.
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final NoGoAreaService service;
    public NoGoController(NoGoAreaService service) { this.service = service; }

    /**
     * 화면 범위 안의 구간만. 전건이 1,276건(약 320KB)이라 지도를 움직일 때마다
     * 전부 내려줄 이유가 없다. bbox 가 없으면 전건을 준다.
     */
    @GetMapping(params = "bbox")
    public ResponseEntity<List<NoGoResponse>> listWithin(@RequestParam String bbox) {
        return cached(service.findAll(parseBbox(bbox)));
    }

    /** 지도 표시용이라 unverified 도 내려간다. 프론트는 verification_status 로 구분한다. */
    @GetMapping public ResponseEntity<List<NoGoResponse>> list() {
        return cached(service.findAll());
    }

    private static ResponseEntity<List<NoGoResponse>> cached(List<NoGoArea> areas) {
        return ResponseEntity.ok().cacheControl(CACHE)
                .body(areas.stream().map(NoGoController::toResponse).toList());
    }

    /**
     * GeoJSON bbox 와 같은 순서인 minLon,minLat,maxLon,maxLat 로 받는다.
     * 이 프로젝트는 밖으로 나가는 좌표가 어디서나 lon 이 먼저다(응답의 path 도 그렇다).
     *
     * 주의: BoundingBox 레코드는 반대로 lat 이 먼저다. 아래 인자 순서를 뒤집으면
     * 조회가 예외 없이 조용히 0건이 된다 — 이 프로젝트가 여러 번 데인 자리다.
     */
    private static BoundingBox parseBbox(String raw) {
        String[] parts = raw.split(",");
        if (parts.length != 4) {
            throw new ValidationException("bbox 는 minLon,minLat,maxLon,maxLat 네 값입니다: " + raw);
        }
        double[] v = new double[4];
        for (int i = 0; i < 4; i++) {
            try {
                v[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new ValidationException("bbox 에 숫자가 아닌 값이 있습니다: " + raw);
            }
        }
        try {
            return new BoundingBox(v[1], v[0], v[3], v[2]);   // (minLat, minLon, maxLat, maxLon)
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
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
