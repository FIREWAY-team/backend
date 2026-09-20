-- UI 차량 제원과 라우팅 DB를 일치시킨다. 기존 V4_1은 이미 배포됐으므로 후속 변경만 한다.
UPDATE vehicles
SET name='소형펌프차', width_m=2.3, height_m=3.0, length_m=7.0,
    weight_ton=3.5, turning_radius_m=6.5
WHERE vehicle_id='pump-3.5';

UPDATE vehicles
SET name='중형펌프차', width_m=2.5, height_m=3.3, length_m=8.0,
    weight_ton=8.0, turning_radius_m=8.0
WHERE vehicle_id='pump-8';

INSERT INTO vehicles
    (vehicle_id, name, width_m, height_m, length_m, weight_ton, turning_radius_m)
VALUES
    ('pump-15', '대형펌프차', 2.9, 3.6, 9.5, 15.0, 10.5),
    ('aerial-25', '25m 굴절차', 2.5, 3.6, 11.5, 18.0, 10.5)
ON DUPLICATE KEY UPDATE
    name=VALUES(name), width_m=VALUES(width_m), height_m=VALUES(height_m),
    length_m=VALUES(length_m), weight_ton=VALUES(weight_ton),
    turning_radius_m=VALUES(turning_radius_m);
