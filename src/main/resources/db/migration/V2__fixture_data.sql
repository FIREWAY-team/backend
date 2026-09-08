INSERT INTO scenarios VALUES
('scenario-001','태평동 시장 골목 화재',37.4441,127.1388,'pump-3.5','[{"routeId":"r1","rank":1},{"routeId":"r2","rank":2},{"routeId":"r3","rank":3}]','{"summary":"시장 진입로 폭과 회차 공간을 고려한 사전 분석"}',CURRENT_TIMESTAMP),
('scenario-002','수진동 주택 밀집지 화재',37.4382,127.1401,'pump-8','[{"routeId":"r4","rank":1},{"routeId":"r5","rank":2},{"routeId":"r6","rank":3}]','{"summary":"대형 펌프차 접근 가능성을 반영한 경로"}',CURRENT_TIMESTAMP),
('scenario-003','신흥동 언덕길 화재',37.4467,127.1542,'pump-3.5','[{"routeId":"r7","rank":1},{"routeId":"r8","rank":2},{"routeId":"r9","rank":3}]','{"summary":"경사와 골목 폭을 고려한 사전 분석"}',CURRENT_TIMESTAMP);
INSERT INTO vehicles VALUES
('pump-3.5','펌프차 3.5톤',2.30,3.10,7.20,3.5,8.0),
('pump-8','펌프차 8톤',2.50,3.40,9.20,8.0,10.0);
INSERT INTO cctv_readings VALUES
('cctv-001','edge-001','https://fireroad.shop/mock/cctv-001.jpg',3.2,0.4,2.8,'[{"type":"parked_car","width_m":0.4}]','{"verdict":"PASS"}',0.94,CURRENT_TIMESTAMP,'mock',0.1,'{"fixture":true}'),
('cctv-002','edge-002','https://fireroad.shop/mock/cctv-002.jpg',2.7,0.8,1.9,'[{"type":"awning","width_m":0.8}]','{"verdict":"CAUTION"}',0.88,CURRENT_TIMESTAMP,'mock',0.1,'{"fixture":true}'),
('cctv-003','edge-003','https://fireroad.shop/mock/cctv-003.jpg',2.4,1.2,1.2,'[{"type":"truck","width_m":1.2}]','{"verdict":"FAIL"}',0.97,CURRENT_TIMESTAMP,'mock',0.1,'{"fixture":true}'),
('cctv-004','edge-004','https://fireroad.shop/mock/cctv-004.jpg',3.5,0.2,3.3,'[{"type":"none","width_m":0.0}]','{"verdict":"PASS"}',0.91,CURRENT_TIMESTAMP,'mock',0.1,'{"fixture":true}'),
('cctv-005','edge-005','https://fireroad.shop/mock/cctv-005.jpg',3.0,0.6,2.4,'[{"type":"pole","width_m":0.6}]','{"verdict":"CAUTION"}',0.86,CURRENT_TIMESTAMP,'mock',0.1,'{"fixture":true}');

