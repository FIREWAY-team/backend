-- 신고 접수 상세(intake). 상황실 카드가 보여줄 신고자·규모·건물 정보.
-- 전화가 끊긴 신고처럼 아무것도 못 받는 경우가 있으므로 전부 NULL 허용이다.
-- 접수시각은 received_at 을 그대로 쓴다.
ALTER TABLE incidents
  ADD COLUMN reporter_name       VARCHAR(50)  NULL AFTER summary,
  ADD COLUMN reporter_phone      VARCHAR(20)  NULL AFTER reporter_name,  -- 시연은 마스킹 값만 넣는다
  ADD COLUMN severity            VARCHAR(8)   NULL AFTER reporter_phone, -- small / medium / large
  ADD COLUMN estimated_area_m2   INT          NULL AFTER severity,
  ADD COLUMN building_type       VARCHAR(100) NULL AFTER estimated_area_m2,
  ADD COLUMN casualties_reported BOOLEAN      NOT NULL DEFAULT FALSE AFTER building_type,
  ADD COLUMN notes               VARCHAR(500) NULL AFTER casualties_reported;
