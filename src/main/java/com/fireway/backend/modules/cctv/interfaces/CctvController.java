package com.fireway.backend.modules.cctv.interfaces;

import com.fireway.backend.modules.cctv.application.CctvService;
import com.fireway.backend.modules.cctv.domain.CctvReading;
import com.fireway.backend.shared.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * CCTV 판독 조회 API.
 * - GET /api/cctv/{id}   판독 상세. 없는 id 는 404.
 * - GET /api/cctv        전체 목록. 지도 마커 렌더용. lat/lon 은 source_meta 파생, 없으면 null.
 *
 * still_public_url 은 프리사인드 GET URL (10분) — 응답 시 발급하고 DB 에 영속하지 않는다.
 */
@RestController
@RequestMapping("/api/cctv")
public class CctvController {

    private final CctvService service;

    public CctvController(CctvService service) { this.service = service; }

    @GetMapping("/{id}")
    public CctvResponse get(@PathVariable String id) {
        CctvReading r = service.get(id).orElseThrow(() -> new NotFoundException("CCTV 판독을 찾을 수 없습니다."));
        return toResponse(service, r);
    }

    @GetMapping
    public List<CctvSummary> list() {
        return service.list().stream().map(CctvController::toSummary).toList();
    }

    private static CctvResponse toResponse(CctvService svc, CctvReading r) {
        String url = svc.resolveMediaUrl(r);
        String mediaStatus = mediaStatus(r, url);
        return new CctvResponse(
                r.cctvId(), url,
                nz(r.wallWidthM()), nz(r.obstacleWidthM()), nz(r.effectiveWidthM()),
                r.verdict() == null ? Map.of() : r.verdict(),
                nz(r.confidence()),
                r.contentType(), mediaStatus, r.measurementStatus(),
                iso(r.measuredAt()), iso(r.lastAttemptedAt()),
                svc.mediaUrlTtlSeconds());
    }

    private static CctvSummary toSummary(CctvReading r) {
        Map<String, String> v = r.verdict() == null ? Map.of() : r.verdict();
        // verdict 는 두 shape · {"status":"PASS"} (옛 fixture) 또는 {"pump-3.5":"PASS","pump-8":"FAIL"} (AI 파이프라인).
        // 마커 색 하나만 필요하므로 status → pump-3.5 → pump-8 → 첫 값 순 fallback.
        String status = v.get("status");
        if (status == null) status = v.get("pump-3.5");
        if (status == null) status = v.get("pump-8");
        if (status == null && !v.isEmpty()) status = v.values().iterator().next();
        return new CctvSummary(r.cctvId(), r.lat(), r.lon(), v, status, r.measurementStatus());
    }

    private static String mediaStatus(CctvReading r, String url) {
        // s3 key 도 원본 http url 도 없으면 미등록.
        boolean hasKey = (r.s3Key() != null && !r.s3Key().isBlank())
                || (r.stillPublicUrl() != null && !r.stillPublicUrl().isBlank());
        if (!hasKey) return "not_registered";
        return url == null ? "failed" : "registered";
    }

    private static double nz(Double v) { return v == null ? 0.0 : v; }

    private static String iso(LocalDateTime dt) {
        return dt == null ? null : dt.toString();
    }

    /**
     * verdict 는 차종별({"pump-3.5":"PASS","pump-8":"UNCERTAIN"}) 또는 옛 shape({"status":"PASS"})
     * 어느 쪽이든 그대로 내려간다 — FE mapper 가 흡수.
     */
    public record CctvResponse(
            String cctvId,
            String stillPublicUrl,
            double wallWidthM,
            double obstacleWidthM,
            double effectiveWidthM,
            Map<String, String> verdict,
            double confidence,
            String contentType,
            String mediaStatus,
            String measurementStatus,
            String measuredAt,
            String measurementFailureAt,
            long mediaUrlExpiresInSeconds) { }

    /** 지도 마커용. lat/lon 이 null 이면 프론트가 그 마커는 스킵한다. */
    public record CctvSummary(
            String cctvId,
            Double lat,
            Double lon,
            Map<String, String> verdict,
            String status,
            String measurementStatus) { }
}
