package com.fireway.backend.modules.cctv.application.port;
import com.fireway.backend.modules.cctv.domain.CctvReading;
import java.util.Optional;
public interface CctvReadingRepository { Optional<CctvReading> findById(String id); }
