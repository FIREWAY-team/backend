INSERT INTO no_go_areas (dong, reason, layer, polygon) VALUES
('은행1동', '공사 중', 1, ST_GeomFromText('POLYGON((127.142 37.438,127.143 37.438,127.144 37.439,127.142 37.438))', 4326, 'axis-order=long-lat')),
('상대원1동', '도로 통제', 1, ST_GeomFromText('POLYGON((127.164 37.431,127.165 37.431,127.165 37.432,127.164 37.431))', 4326, 'axis-order=long-lat'));
