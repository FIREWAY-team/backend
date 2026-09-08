INSERT INTO cctv_readings (cctv_id, edge_id, still_public_url, wall_width_m, obstacle_width_m, effective_width_m, detected_objects, verdict, confidence, measured_at, method, calibration_error_m, source_meta) VALUES
('cctv-01','edge-01','https://example.com/mock/cctv-01.jpg',5.2,1.1,4.1,JSON_ARRAY('truck'),JSON_OBJECT('status','PASS'),0.94,NOW(),'mock',0.1,JSON_OBJECT('source','fixture')),
('cctv-02','edge-02','https://example.com/mock/cctv-02.jpg',4.8,1.5,3.3,JSON_ARRAY('car'),JSON_OBJECT('status','PASS'),0.91,NOW(),'mock',0.1,JSON_OBJECT('source','fixture')),
('cctv-03','edge-03','https://example.com/mock/cctv-03.jpg',3.6,2.0,1.6,JSON_ARRAY('bollard'),JSON_OBJECT('status','FAIL'),0.89,NOW(),'mock',0.2,JSON_OBJECT('source','fixture')),
('cctv-04','edge-04','https://example.com/mock/cctv-04.jpg',5.0,0.4,4.6,JSON_ARRAY(),JSON_OBJECT('status','PASS'),0.97,NOW(),'mock',0.1,JSON_OBJECT('source','fixture')),
('cctv-05','edge-05','https://example.com/mock/cctv-05.jpg',4.2,1.0,3.2,JSON_ARRAY('bus'),JSON_OBJECT('status','PASS'),0.88,NOW(),'mock',0.2,JSON_OBJECT('source','fixture'));
