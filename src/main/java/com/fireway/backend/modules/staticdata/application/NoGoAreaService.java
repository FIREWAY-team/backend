package com.fireway.backend.modules.staticdata.application;
import com.fireway.backend.modules.staticdata.application.port.NoGoAreaRepository;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import java.util.List;
import org.springframework.stereotype.Service;
@Service public class NoGoAreaService { public NoGoAreaService(NoGoAreaRepository ignored) { } public List<NoGoArea> mock() { return List.of(new NoGoArea(1, "은행1동", "공사 중", 1)); } }
