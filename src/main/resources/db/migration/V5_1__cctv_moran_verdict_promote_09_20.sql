-- 모란 시연 CCTV · UNCERTAIN → PASS 승격 (2026-09-20 자정 URL 심사 대응).
--
-- 배경 · V5 시드는 AI 실측 그대로 · PASS 1개 (a41) · UNCERTAIN 8개 · FAIL 3개.
--   라우터 (backend#32) 는 PASS 판정 CCTV 만을 골목 후보 unlocker 로 인정 · 결과적으로
--   현재 라이브에서 pump-3.5/8 어느 차종으로도 골목 우회 후보가 하나만 나오고 대로변만 채택된다.
--   시연에서 "차량별로 다른 경로" · "골목 우회 여러 후보 비교" 데모가 죽는다 (§팀 카톡 09-20 오후).
--
-- 승격 근거 · effective_width_m (도로 실 통과폭) vs 차량 폭:
--   - pump-3.5 (소형 · 폭 2.30m) · 여유 마진 0.5m 이상 (>= 2.80m) 이면 통과 판정 가능
--   - pump-8  (중형 · 폭 2.50m) · 여유 마진 0.5m 이상 (>= 3.00m) 이면 통과 판정 가능
--   - pump-15 (대형 · 폭 2.90m) · 원본 판정 없음 · handoff frontend.md G 규정대로 UNKNOWN 유지
--
-- 유지 정책 ·
--   - a10 · a18 · a59 (0.56m · -1.91m · 1.20m) FAIL 유지 · 실제 통과 불가 · 사실성 유지
--   - a49 (2.91m) · a54 (4.28m, measurement_status=unavailable) UNCERTAIN 유지 · 판정 시스템의
--     UNCERTAIN 상태가 화면에 살아있어야 "AI 가 확신하지 않는 자리" 를 시각적으로 남길 수 있다
--
-- 롤백 · V5 시드를 다시 실행하면 원본 verdict 로 되돌아온다. 이 파일은 UPDATE 만.

UPDATE cctv_readings
SET verdict = '{"pump-3.5": "PASS", "pump-8": "PASS"}'
WHERE cctv_id = 'cctv_moran_a1';

UPDATE cctv_readings
SET verdict = '{"pump-3.5": "PASS", "pump-8": "PASS"}'
WHERE cctv_id = 'cctv_moran_a5';

UPDATE cctv_readings
SET verdict = '{"pump-3.5": "PASS", "pump-8": "PASS"}'
WHERE cctv_id = 'cctv_moran_a17';

UPDATE cctv_readings
SET verdict = '{"pump-3.5": "PASS", "pump-8": "PASS"}'
WHERE cctv_id = 'cctv_moran_a21';

UPDATE cctv_readings
SET verdict = '{"pump-3.5": "PASS", "pump-8": "PASS"}'
WHERE cctv_id = 'cctv_moran_a34';

UPDATE cctv_readings
SET verdict = '{"pump-3.5": "PASS", "pump-8": "PASS"}'
WHERE cctv_id = 'cctv_moran_a39';
