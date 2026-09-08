package com.fireway.backend.modules.cctv.interfaces;
import com.fireway.backend.shared.exception.NotFoundException;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/cctv") public class CctvController {
    @GetMapping("/{id}") public CctvResponse get(@PathVariable String id) { return new CctvResponse(id, "https://example.com/mock/cctv-01.jpg", 5.2, 1.1, 4.1, "PASS", 0.94); }
    public record CctvResponse(String cctvId, String stillPublicUrl, double wallWidthM, double obstacleWidthM, double effectiveWidthM, String verdict, double confidence) { }
}
