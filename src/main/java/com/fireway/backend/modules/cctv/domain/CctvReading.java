package com.fireway.backend.modules.cctv.domain;

import java.time.LocalDateTime;
import java.util.Map;

// verdict 는 AI 파이프라인이 cctv_readings.verdict(JSON) 에 {"pump-3.5":"PASS","pump-8":"FAIL"}
// 형태로 채운다. 옛 fixture 의 {"status":"PASS"} 도 adapter 가 흡수해 여기서는 항상 통일된 Map.
// lat/lon/s3Key/contentType 은 source_meta JSON 에서 추출. 없으면 null — 서비스가 fallback 처리.
public record CctvReading(
        String cctvId,
        Double effectiveWidthM,
        Double wallWidthM,
        Double obstacleWidthM,
        Map<String, String> verdict,
        Double confidence,
        LocalDateTime measuredAt,
        LocalDateTime lastAttemptedAt,
        Double lat,
        Double lon,
        String s3Key,
        String contentType,
        String measurementStatus,
        String stillPublicUrl,
        DemoAssignment demoAssignment) {

    public record DemoAssignment(
            String evidenceCctvId,
            boolean sharedPassFootage,
            boolean reassigned) { }
}
