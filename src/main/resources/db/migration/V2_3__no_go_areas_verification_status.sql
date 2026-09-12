-- note 하나에 "검증 상태"와 "현장 사유" 두 의미를 겹쳐 싣고 있었다.
-- CCTV 판독 결과로 note 를 UPDATE 하기로 한 이상, 라우팅 필터를 note LIKE 로 두면
-- UPDATE 한 번에 조용히 뚫린다. 상태를 별도 컬럼으로 뺀다.
--   ok         - 그대로 써도 되는 구간
--   unverified - 위성 대조에서 실제 도로가 아닌 것으로 확인됨 (재개발로 사라진 옛 골목 등)
ALTER TABLE no_go_areas
  ADD COLUMN verification_status VARCHAR(24) NOT NULL DEFAULT 'ok' AFTER layer;

-- 기존 행 백필. 이 시점에서 note 가 비어있지 않은 행은 전부 "지도상 미확인" 34건이다.
-- 한글 리터럴로 비교하면 마이그레이션 파일 인코딩이 틀어졌을 때 조용히 0건이 되므로 쓰지 않는다.
-- 이후로는 import_no_go.py 가 GeoJSON 의 note 를 보고 직접 넣는다.
UPDATE no_go_areas SET verification_status = 'unverified' WHERE note <> '';

-- 라우팅 조회가 항상 이 조건으로 들어온다.
CREATE INDEX ix_no_go_areas_verification ON no_go_areas (verification_status);
