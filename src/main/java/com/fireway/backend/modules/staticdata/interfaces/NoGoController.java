package com.fireway.backend.modules.staticdata.interfaces;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/no_go") public class NoGoController { @GetMapping public List<NoGoResponse> list() { return List.of(new NoGoResponse(1L, "은행1동", "공사 중", 1, List.of(List.of(37.438, 127.142), List.of(37.439, 127.143), List.of(37.438, 127.144)))); } public record NoGoResponse(long id, String dong, String reason, int layer, List<List<Double>> polygon) { } }
