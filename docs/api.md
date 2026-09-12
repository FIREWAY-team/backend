# API v2

모든 JSON 필드는 snake_case입니다. mock 단계에서는 fixture와 동일한 대표 응답을 반환합니다.

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/scenarios` | 화재 시나리오 3건 |
| POST | `/api/route` | scenario_id, lat, lon, vehicle_id로 사전계산 경로 조회 |
| GET | `/api/no_go` | 진입곤란 구간과 사유 (mock 아님, DB 조회) |
| GET | `/api/cctv/{id}` | CCTV 판독·유효 폭·confidence |
| GET | `/api/vehicles` | pump-3.5, pump-8 차량 제원 |
| GET | `/api/fire-system/nearest?lat=&lon=` | 외부 소방 시스템 mock 연계 |
| POST | `/api/files/upload-url` | 이미지 업로드용 presigned URL 발급 |

`POST /api/route` 요청 예: `{ "scenario_id": "bank-01", "lat": 37.4381, "lon": 127.1422, "vehicle_id": "pump-3.5" }`.

## GET /api/no_go

중원구청 관내도에서 추출한 소방차 진입곤란 구간입니다. mock이 아니라 `no_go_areas` 테이블을 그대로 읽습니다.

```json
[
  {
    "id": 3,
    "ext_id": "eunhaeng1-impassable-001",
    "dong": "은행1동",
    "reason": "소방차 진입곤란 지정",
    "layer": 1,
    "verification_status": "ok",
    "note": "",
    "geometry_type": "LineString",
    "path": [[127.142, 37.438], [127.143, 37.439]]
  }
]
```

- **`path` 는 `[lon, lat]` 순서입니다.** GeoJSON 표준과 같고, DB의 WKT·임포트 스크립트와도 축 순서가 일치합니다.
  V2_2 이전 mock 응답은 `[lat, lon]` 순서의 `polygon` 필드였습니다. 필드명과 순서가 함께 바뀐 breaking change입니다.
- `geometry_type` 은 `"LineString"` 또는 `"Polygon"` 입니다. 진입곤란 도로는 면이 아니라 중심선이라
  임포트분은 전부 `LineString` 이고, V2_1 픽스처로 들어간 2건만 `Polygon` 입니다.
  `Polygon` 은 외곽 링 좌표만 내려갑니다.
- `ext_id` 는 임포트 원본의 식별자(`<동>-impassable-<번호>`)이고, 픽스처 행은 `null` 입니다.
- `note` 는 1,240건이 빈 문자열입니다. 관내도에 현장 사유 정보가 없어서,
  현재 값이 있는 것은 아래 `unverified` 34건뿐입니다.
- **`verification_status`** 는 `"ok"` 또는 `"unverified"` 입니다. `unverified` 34건은 위성 대조에서
  실제 도로가 아닌 것으로 확인된 구간입니다(재개발로 사라진 옛 골목). 건물 위에 그려져 있어서
  라우팅에 넣으면 가까운 실제 진입로를 잘못 막습니다. **라우팅은 `NoGoAreaService.forRouting()`** 을
  쓰고, 이 목록에는 `ok` 만 들어갑니다.
- 이 엔드포인트는 지도 표시용이라 `unverified` 도 그대로 내려갑니다. 프론트는 점선 오버레이 +
  "재검증 필요" 배지로 구분합니다. 숨기려면 `verification_status` 로 거르면 됩니다.
- 판정은 `note` 가 아니라 `verification_status` 로 합니다. `note` 는 나중에 CCTV 판독 결과 등
  현장 사유로 덮일 예정이라, 거기에 필터를 걸면 UPDATE 한 번에 뚫립니다(V2_3에서 분리한 이유).
- 필터 파라미터가 없어 1,274건을 한 번에 내려줍니다. 본문은 약 321KB지만 gzip 이 켜져 있어
  실제 전송량은 약 39KB입니다(`server.compression`). 잘라서 받고 싶으면 도엽 이름(`dong`)이 아니라
  지도 영역(bbox) 기준이어야 합니다 — `dong` 값은 동 이름이 아니라 관내도 도엽 이름입니다
  (`상대원1동1`, `성남동1(모란역부근)` 처럼). 아직 필터는 없습니다.
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
