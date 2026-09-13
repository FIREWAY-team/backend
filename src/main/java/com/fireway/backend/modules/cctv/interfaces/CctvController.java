package com.fireway.backend.modules.cctv.interfaces;
import com.fireway.backend.shared.exception.NotFoundException;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/cctv") public class CctvController {
    @GetMapping("/{id}") public CctvResponse get(@PathVariable String id) { return new CctvResponse(id, "https://example.com/mock/cctv-01.jpg", 5.2, 1.1, 4.1, Map.of("pump-3.5", "PASS", "pump-8", "UNCERTAIN"), 0.94); }
    // verdict는 차종별(pump-3.5, pump-8) 동시 판정 — AI 파이프라인이 채우는 cctv_readings.verdict(JSON)와 형태를 맞춘다.
    public record CctvResponse(String cctvId, String stillPublicUrl, double wallWidthM, double obstacleWidthM, double effectiveWidthM, Map<String, String> verdict, double confidence) { }
}
