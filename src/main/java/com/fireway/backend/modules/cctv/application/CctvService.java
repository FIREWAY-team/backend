package com.fireway.backend.modules.cctv.application;
import com.fireway.backend.modules.cctv.application.port.CctvReadingRepository;
import com.fireway.backend.modules.cctv.domain.CctvReading;
import java.util.Map;
import org.springframework.stereotype.Service;
@Service public class CctvService { public CctvService(CctvReadingRepository ignored) { } public CctvReading mock(String id) { return new CctvReading(id, 4.1, Map.of("pump-3.5", "PASS", "pump-8", "UNCERTAIN"), .94); } }
