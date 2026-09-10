# API v2

모든 JSON 필드는 snake_case입니다. mock 단계에서는 fixture와 동일한 대표 응답을 반환합니다.

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/scenarios` | 화재 시나리오 3건 |
| POST | `/api/route` | scenario_id, lat, lon, vehicle_id로 사전계산 경로 조회 |
| GET | `/api/no_go` | No-Go 폴리곤과 사유 |
| GET | `/api/cctv/{id}` | CCTV 판독·유효 폭·confidence |
| GET | `/api/vehicles` | pump-3.5, pump-8 차량 제원 |
| GET | `/api/fire-system/nearest?lat=&lon=` | 외부 소방 시스템 mock 연계 |
| POST | `/api/files/upload-url` | 이미지 업로드용 presigned URL 발급 |

`POST /api/route` 요청 예: `{ "scenario_id": "bank-01", "lat": 37.4381, "lon": 127.1422, "vehicle_id": "pump-3.5" }`.

## 파일 업로드

`POST /api/files/upload-url` 요청 예: `{ "content_type": "image/jpeg" }`.
허용 타입은 `image/jpeg`, `image/png`, `image/webp` 이고 그 밖은 422 입니다.

응답 예:

```json
{ "upload_url": "https://<bucket>.s3.ap-northeast-2.amazonaws.com/uploads/2026-09-09/<uuid>?X-Amz-...",
  "key": "uploads/2026-09-09/<uuid>",
  "expires_in_seconds": 300 }
```

클라이언트는 받은 `upload_url` 로 **직접 PUT** 합니다(파일 바이트는 백엔드를 거치지 않습니다).
이때 `Content-Type` 헤더는 발급 요청에 쓴 값과 같아야 합니다 — 서명 대상이라 다르면 S3가 403 입니다.

업로드가 끝나면 `key` 만 백엔드로 보냅니다. 저장하는 쪽 도메인은 DB에 넣기 전에
`FileUploadService.confirmUpload(key)` 로 실제 존재와 크기를 확인해야 합니다 —
프론트의 "올렸어요" 보고만 믿으면 유령 레코드가 생깁니다.

업로드 상한은 5MB 입니다. presigned PUT 자체는 크기를 막지 못하므로,
`confirmUpload()` 가 올라온 뒤 HEAD 로 재보고 초과분은 지운 다음 422 를 돌려줍니다.
발급 API 남용은 Nginx rate limit(IP당 분당 5회)과 S3 수명주기 규칙(`uploads/` 30일 만료)이 함께 막습니다.

조회는 `FileUploadService.issueDownloadUrl(key)` 로 그때그때 만들어 내려줍니다.
DB에는 URL이 아니라 **key만** 저장합니다(버킷을 옮기거나 CloudFront를 붙여도 데이터가 안 썩습니다).

로컬(prod 프로파일이 아닐 때)은 S3 대신 `LocalStorageAdapter` 가 붙어 `build/local-storage/` 에
파일을 두고 `/local-storage/**` 로 서빙합니다. AWS 자격증명이 없어도 됩니다.
