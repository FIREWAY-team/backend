-- 모란 12곳을 data/geo/impassable_all.geojson의 최근접 진입곤란 구간에 연결한다.
-- cctv_readings.edge_id는 V3부터 존재하는 매핑 컬럼이며 라우팅의 polygon_id(ext_id)와 같다.
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-056' WHERE cctv_id='cctv_moran_a18';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-056' WHERE cctv_id='cctv_moran_a39';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-061' WHERE cctv_id='cctv_moran_a21';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-056' WHERE cctv_id='cctv_moran_a10';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-062' WHERE cctv_id='cctv_moran_a17';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-062' WHERE cctv_id='cctv_moran_a41';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-047' WHERE cctv_id='cctv_moran_a34';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-069' WHERE cctv_id='cctv_moran_a54';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-064' WHERE cctv_id='cctv_moran_a49';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-059' WHERE cctv_id='cctv_moran_a1';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-055' WHERE cctv_id='cctv_moran_a59';
UPDATE cctv_readings SET edge_id='seongnam1-moran-impassable-063' WHERE cctv_id='cctv_moran_a5';

-- A41의 영상·측정값·판정 근거를 시연용 PASS 지점 4곳에 함께 복제한다.
-- 위치(lat/lon/address)는 각 대상 CCTV 값을 보존하고 demo_assignment로 재사용임을 명시한다.
UPDATE cctv_readings AS target
JOIN cctv_readings AS source ON source.cctv_id='cctv_moran_a41'
SET target.still_public_url=source.still_public_url,
    target.wall_width_m=source.wall_width_m,
    target.obstacle_width_m=source.obstacle_width_m,
    target.effective_width_m=source.effective_width_m,
    target.detected_objects=source.detected_objects,
    target.confidence=source.confidence,
    target.measured_at=source.measured_at,
    target.method=source.method,
    target.calibration_error_m=source.calibration_error_m,
    target.source_meta=JSON_SET(
        COALESCE(target.source_meta, JSON_OBJECT()),
        '$.s3_media', JSON_EXTRACT(source.source_meta, '$.s3_media'),
        '$.measurement_status', JSON_UNQUOTE(JSON_EXTRACT(source.source_meta, '$.measurement_status')),
        '$.footage_mode', 'shared_pass_footage',
        '$.footage_note', 'A41 원본 영상·측정값·판정 근거를 재사용한 시연 데이터',
        '$.demo_assignment', JSON_OBJECT(
            'evidence_cctv_id', 'cctv_moran_a41',
            'shared_pass_footage', TRUE,
            'reassigned', TRUE
        )
    )
WHERE target.cctv_id IN ('cctv_moran_a1', 'cctv_moran_a5', 'cctv_moran_a17', 'cctv_moran_a34');

-- 유강현 확정 차종별 판정표. 폭이 같은 pump-8/aerial-25는 같은 결과를 사용한다.
UPDATE cctv_readings
SET verdict=CASE cctv_id
    WHEN 'cctv_moran_a1'  THEN '{"pump-3.5":"PASS","pump-8":"PASS","pump-15":"PASS","aerial-25":"PASS"}'
    WHEN 'cctv_moran_a5'  THEN '{"pump-3.5":"UNCERTAIN","pump-8":"UNCERTAIN","pump-15":"FAIL","aerial-25":"UNCERTAIN"}'
    WHEN 'cctv_moran_a10' THEN '{"pump-3.5":"FAIL","pump-8":"FAIL","pump-15":"FAIL","aerial-25":"FAIL"}'
    WHEN 'cctv_moran_a17' THEN '{"pump-3.5":"PASS","pump-8":"PASS","pump-15":"PASS","aerial-25":"PASS"}'
    WHEN 'cctv_moran_a18' THEN '{"pump-3.5":"FAIL","pump-8":"FAIL","pump-15":"FAIL","aerial-25":"FAIL"}'
    WHEN 'cctv_moran_a21' THEN '{"pump-3.5":"PASS","pump-8":"PASS","pump-15":"UNCERTAIN","aerial-25":"PASS"}'
    WHEN 'cctv_moran_a34' THEN '{"pump-3.5":"PASS","pump-8":"UNCERTAIN","pump-15":"FAIL","aerial-25":"UNCERTAIN"}'
    WHEN 'cctv_moran_a39' THEN '{"pump-3.5":"UNCERTAIN","pump-8":"UNCERTAIN","pump-15":"FAIL","aerial-25":"UNCERTAIN"}'
    WHEN 'cctv_moran_a41' THEN '{"pump-3.5":"PASS","pump-8":"PASS","pump-15":"PASS","aerial-25":"PASS"}'
    WHEN 'cctv_moran_a49' THEN '{"pump-3.5":"PASS","pump-8":"PASS","pump-15":"PASS","aerial-25":"PASS"}'
    WHEN 'cctv_moran_a54' THEN '{"pump-3.5":"UNCERTAIN","pump-8":"UNCERTAIN","pump-15":"UNCERTAIN","aerial-25":"UNCERTAIN"}'
    WHEN 'cctv_moran_a59' THEN '{"pump-3.5":"FAIL","pump-8":"FAIL","pump-15":"FAIL","aerial-25":"FAIL"}'
END
WHERE cctv_id IN (
    'cctv_moran_a1', 'cctv_moran_a5', 'cctv_moran_a10', 'cctv_moran_a17',
    'cctv_moran_a18', 'cctv_moran_a21', 'cctv_moran_a34', 'cctv_moran_a39',
    'cctv_moran_a41', 'cctv_moran_a49', 'cctv_moran_a54', 'cctv_moran_a59'
);
