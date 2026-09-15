-- 신고 접수. V5 를 새로 배정받는 대신 V2(이태연) 서브버전으로 간다.
-- CONTRIBUTING 이 막는 것은 "남의 major" 이고, 미배정 major 를 먼저 집는 쪽이
-- 오히려 다른 사람과 부딪힐 여지가 있다.
CREATE TABLE incidents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  incident_no VARCHAR(24) NOT NULL,              -- 접수번호 2026-0915-0001
  status VARCHAR(24) NOT NULL DEFAULT 'RECEIVED',
  -- RECEIVED 접수 / DISPATCHED 지령 / ON_SCENE 현장도착 / CLOSED 종결 / CANCELLED 오인·취소

  address VARCHAR(200),
  lat DOUBLE NOT NULL,
  lon DOUBLE NOT NULL,
  summary VARCHAR(500) NOT NULL DEFAULT '',

  received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  closed_at   DATETIME NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_incidents_no (incident_no),
  -- 목록 조회가 "상태로 거르고 접수 최신순" 한 가지뿐이라 그 모양대로 건다.
  -- 상태 단독으로는 선택도가 낮지만 received_at 이 붙어 정렬까지 받는다.
  -- (V2_3 에서 선택도 없는 단일 컬럼 인덱스를 걸었다가 공간 인덱스를 막은 적이 있다.)
  KEY ix_incidents_status_received (status, received_at)
);

-- 좌표는 POINT 가 아니라 lat/lon DOUBLE 로 둔다. scenarios 와 같은 방식이고,
-- 축 순서로 이미 여러 번 데인 만큼 공간 타입을 신고 쪽까지 늘릴 이유가 없다.
-- 반경 검색이 필요해지면 질의 시점에 ST_Distance_Sphere 로 만들어 쓴다.
