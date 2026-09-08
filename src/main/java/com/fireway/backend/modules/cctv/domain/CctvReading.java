package com.fireway.backend.modules.cctv.domain;
public record CctvReading(String cctvId, double effectiveWidthM, String verdict, double confidence) { }
