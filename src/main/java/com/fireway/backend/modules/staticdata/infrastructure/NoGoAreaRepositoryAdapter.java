package com.fireway.backend.modules.staticdata.infrastructure;
import com.fireway.backend.modules.staticdata.application.port.NoGoAreaRepository;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import java.util.List;
import org.springframework.stereotype.Component;
@Component public class NoGoAreaRepositoryAdapter implements NoGoAreaRepository { public List<NoGoArea> findAll() { return List.of(); } }
