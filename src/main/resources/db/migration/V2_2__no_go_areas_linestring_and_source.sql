-- 진입곤란 도로는 면이 아니라 도로 중심선이다.
-- V2 에서 polygon 으로 만들었지만 실제로 들어가는 건 LineString 이므로 이름을 맞춘다.
-- 타입은 GEOMETRY 그대로라 V2_1 로 들어간 기존 폴리곤 2건도 그대로 유효하다.
ALTER TABLE no_go_areas CHANGE COLUMN polygon geom GEOMETRY NOT NULL SRID 4326;

-- 중원구청 관내도 PDF -> GeoJSON 임포트 출처 추적
ALTER TABLE no_go_areas
  ADD COLUMN ext_id      VARCHAR(64)  NULL AFTER id,
  ADD COLUMN source_pdf  VARCHAR(128) NULL,
  ADD COLUMN source_page INT          NULL,
  ADD COLUMN note        VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN georef_median_error_m DECIMAL(6,2) NULL,
  ADD COLUMN georef_p90_error_m    DECIMAL(6,2) NULL;

-- 임포트를 여러 번 돌려도 중복이 쌓이지 않게 하는 upsert 키.
-- ext_id 는 GeoJSON properties.id (예: eunhaeng1-impassable-001).
CREATE UNIQUE INDEX uk_no_go_areas_ext_id ON no_go_areas (ext_id);
