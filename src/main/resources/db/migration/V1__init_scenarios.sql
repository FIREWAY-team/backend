CREATE TABLE scenarios (
  scenario_id VARCHAR(64) PRIMARY KEY, title VARCHAR(200), fire_lat DOUBLE, fire_lon DOUBLE,
  vehicle_hint VARCHAR(32), routes_precomputed JSON, explanation_precomputed JSON,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
