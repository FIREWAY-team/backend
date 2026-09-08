package com.fireway.backend.modules.cctv.infrastructure;
import com.fireway.backend.modules.cctv.application.port.CctvReadingRepository;
import com.fireway.backend.modules.cctv.domain.CctvReading;
import java.util.Optional;
import org.springframework.stereotype.Component;
@Component public class CctvReadingRepositoryAdapter implements CctvReadingRepository { public Optional<CctvReading> findById(String id) { return Optional.empty(); } }
