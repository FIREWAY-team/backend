package com.fireway.backend.modules.cctv.domain;
import java.util.Map;
// verdict는 차종별(pump-3.5, pump-8) 동시 판정 — AI 파이프라인이 cctv_readings.verdict
// 컬럼(JSON)에 {"pump-3.5": "PASS", "pump-8": "FAIL"} 형태로 채운다. 단일 status 문자열이 아니다.
public record CctvReading(String cctvId, double effectiveWidthM, Map<String, String> verdict, double confidence) { }
