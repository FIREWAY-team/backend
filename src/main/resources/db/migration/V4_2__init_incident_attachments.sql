-- 신고에 붙는 사진·동영상. 파일 자체는 S3 에 있고 여기엔 key 만 남긴다.
-- URL 을 저장하지 않는 이유: presigned URL 은 몇 분이면 만료되고, 버킷을 옮기면 데이터가 썩는다.
--
-- incidents 는 V2(이태연) 담당이라 ALTER 하지 않고 별도 테이블로 둔다. 번호는 CONTRIBUTING 대로
-- 작성자(윤종호) major 인 V4 의 서브버전이다.
CREATE TABLE incident_attachments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  incident_id  BIGINT NOT NULL,              -- incidents.id (V2_5). FK 는 걸지 않는다(incident_routes 와 같은 방식)
  file_key     VARCHAR(100) NOT NULL,        -- uploads/2026-09-16/<uuid>. 형식은 FileUploadService 가 검사한다
  content_type VARCHAR(100) NULL,            -- S3 HEAD 값. 로컬 저장소는 타입을 못 읽어 NULL 일 수 있다
  size_bytes   BIGINT NOT NULL,              -- 첨부 시점에 HEAD 로 잰 크기
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- 파일 하나는 한 번만 붙는다. 같은 key 를 다른 신고에 붙여 남의 첨부를 끌어오는 것도 막는다.
  UNIQUE KEY uk_incident_attachments_file_key (file_key),
  -- 조회는 항상 "이 신고의 첨부를 붙인 순서대로" 한 가지다.
  KEY ix_incident_attachments_incident (incident_id, id)
);
