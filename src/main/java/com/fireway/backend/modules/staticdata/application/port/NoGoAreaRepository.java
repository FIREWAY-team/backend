package com.fireway.backend.modules.staticdata.application.port;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import java.util.List;
public interface NoGoAreaRepository { List<NoGoArea> findAll(); }
