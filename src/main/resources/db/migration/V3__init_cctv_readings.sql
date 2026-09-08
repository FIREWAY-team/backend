CREATE TABLE cctv_readings (
  cctv_id VARCHAR(64) PRIMARY KEY, edge_id VARCHAR(64), still_public_url VARCHAR(500),
  wall_width_m FLOAT, obstacle_width_m FLOAT, effective_width_m FLOAT, detected_objects JSON,
  verdict JSON, confidence FLOAT, measured_at DATETIME, method VARCHAR(64), calibration_error_m FLOAT, source_meta JSON
);
