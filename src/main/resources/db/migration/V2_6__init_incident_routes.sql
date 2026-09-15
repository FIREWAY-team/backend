-- 신고 한 건에 대해 산출한 경로와 그 판단 근거를 남긴다.
--
-- 라우팅은 계산 결과를 응답으로 한 번 뱉고 끝이라, "왜 이 경로였는지"가 휘발된다.
-- 안전 제품에서 판단 근거가 안 남으면 나중에 설명할 방법이 없다.
CREATE TABLE incident_routes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  incident_id BIGINT NOT NULL,
  vehicle_id  VARCHAR(32) NOT NULL,          -- vehicles.vehicle_id (V4). FK 는 걸지 않는다
  rank_no     TINYINT NOT NULL,              -- 후보 순위. 1이 권장 경로

  distance_m   INT NULL,
  eta_seconds  INT NULL,
  polyline     MEDIUMTEXT NULL,              -- Valhalla encoded polyline

  -- 3층 판정 결과. 이 값들이 "왜 이 경로인지"의 본체다.
  passable_for_vehicle TINYINT(1) NOT NULL DEFAULT 1,
  meets_golden_time    TINYINT(1) NOT NULL DEFAULT 0,
  passable_prob        DECIMAL(4,3) NULL,
  explanation          VARCHAR(500) NOT NULL DEFAULT '',
  excluded_reasons     JSON NULL,            -- 막힌 구간 [{polygon_id, reason, evidence_url}]
  unlocked_by_cctv     JSON NULL,            -- CCTV 판독으로 풀린 구간 id 목록

  planned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- 조회는 항상 "이 신고의 경로를 순위대로" 한 가지다. 그 모양대로만 건다.
  KEY ix_incident_routes_incident_rank (incident_id, rank_no)
);
