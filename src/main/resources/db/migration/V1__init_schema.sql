CREATE TABLE scenarios (
  scenario_id VARCHAR(64) PRIMARY KEY, title VARCHAR(200), fire_lat DOUBLE, fire_lon DOUBLE,
  vehicle_hint VARCHAR(32), routes_precomputed JSON, explanation_precomputed JSON,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE no_go_areas (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, dong VARCHAR(64), reason TEXT, layer TINYINT,
  polygon GEOMETRY NOT NULL SRID 4326, SPATIAL INDEX (polygon)
);
CREATE TABLE cctv_readings (
  cctv_id VARCHAR(64) PRIMARY KEY, edge_id VARCHAR(64), still_public_url VARCHAR(500),
  wall_width_m FLOAT, obstacle_width_m FLOAT, effective_width_m FLOAT, detected_objects JSON,
  verdict JSON, confidence FLOAT, measured_at DATETIME, method VARCHAR(64), calibration_error_m FLOAT,
  source_meta JSON
);
CREATE TABLE vehicles (
  vehicle_id VARCHAR(32) PRIMARY KEY, name VARCHAR(100), width_m FLOAT, height_m FLOAT,
  length_m FLOAT, weight_ton FLOAT, turning_radius_m FLOAT
);

