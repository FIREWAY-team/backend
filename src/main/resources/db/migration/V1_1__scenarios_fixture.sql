INSERT INTO scenarios (scenario_id, title, fire_lat, fire_lon, vehicle_hint, routes_precomputed, explanation_precomputed) VALUES
('bank-01', '은행1동 화재', 37.4381, 127.1422, 'pump-3.5', JSON_ARRAY(), JSON_OBJECT('source','mock')),
('sangdaewon-01', '상대원1동 화재', 37.4311, 127.1642, 'pump-8', JSON_ARRAY(), JSON_OBJECT('source','mock')),
('moran-01', '모란시장 화재', 37.4324, 127.1299, 'pump-3.5', JSON_ARRAY(), JSON_OBJECT('source','mock'));
