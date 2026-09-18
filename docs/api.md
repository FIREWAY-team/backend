# API — Routing

## POST /api/route

전 경로 탐색: 관할 소방서 → 화점 앞 정차점까지 차량 제원·정적 진입곤란·(추후) CCTV 판독을 결합해 최대 3개 후보를 반환합니다. **5분(300초) 골든타임 만족 후보를 상위로 정렬**합니다.

기본 프로필은 OSRM 으로 실제 도로망을 조회합니다(mock 지오메트리가 아닙니다). 진입곤란 구간은 DB 실데이터이고,
CCTV 판독만 아직 차량별 목데이터입니다 — 응답 `warnings` 가 매번 어느 부분이 목인지 밝힙니다.

경로가 없을 때와 라우터를 못 불렀을 때는 다릅니다.

| 상황 | 응답 |
| --- | --- |
| OSRM 이 "그 사이에 길이 없다"고 답함 | 200, `alternatives_status: "no_alternative"`, 빈 `routes` |
| OSRM 을 아예 못 부름(장애·타임아웃·차단) | **502** `EXTERNAL_SYSTEM_ERROR` |

둘을 같은 응답으로 내보내면 프론트가 라우터 장애를 "진입 불가"로 그립니다. 반드시 502 를 따로 처리하세요.

### Request

Content-Type: `application/json`

```json
{
  "vehicle_id": "pump-3.5",
  "from": {"lat": 37.44, "lon": 127.14},
  "to": {"lat": 37.45, "lon": 127.16},
  "k": 3,
  "overlap_threshold": 0.65,
  "golden_time_sec": 300
}
```

| 필드 | 타입 | 필수 / 기본값 | 의미 |
| --- | --- | --- | --- |
| vehicle_id | string | 필수, 공백 불가 | vehicles ID. 기존 fixture: `pump-3.5`, `pump-8` |
| from | Coordinate | 필수 | 출발 위치 |
| to | Coordinate | 필수 | 도착 위치 |
| k | integer | 생략/null 시 3 | 반환 상한, 1~3 |
| overlap_threshold | number | 생략/null 시 0.65 | 0~1. 기존 채택 후보와 겹침이 이 값을 **초과**하면 제거 |
| golden_time_sec | integer | 생략/null 시 300 | 양의 정수. ETA가 이 값 이하이면 골든타임 만족 |

Coordinate: `{"lat": number, "lon": number}`. 두 값 모두 필수이며 위도 -90~90, 경도 -180~180입니다.

### Response

HTTP 200. 아래는 타입을 나타내는 응답 스키마입니다.

```typescript
type RouteResponse = {
  routes: RouteCandidateDto[];
  calc_time_ms: number;          // 서버 계산 시간, 정수 밀리초
  k_effective: number;           // 필터·정렬·K 제한 후 실제 반환 개수
  overlap_matrix: number[][];    // 최종 routes 순서의 k_effective × k_effective 행렬
  alternatives_status: "normal" | "partial" | "no_alternative";
};

type RouteCandidateDto = {
  rank: number;                  // 1부터 연속 순위
  coordinates: [number, number][]; // [[lon, lat], ...]
  polyline: string;              // Valhalla polyline6, 정밀도 10^-6도
  eta_sec: number;               // 정수 초 (실 Valhalla 소수 초는 올림)
  distance_m: number;            // 미터
  passable_prob: number;         // 0~1. 현재 CCTV 미연동 placeholder 1.0
  meets_golden_time: boolean;
  explanation: string;
  excluded_reasons: ExcludedReasonDto[];
};

type ExcludedReasonDto = {
  polygon_id: string;
  reason: string;
  evidence_url: string | null;
};
```

현재 기본 mock은 ETA 200/280/340초인 후보 3개를 생성합니다. 기본 요청에서는 앞의 두 후보가 골든타임을 만족합니다. 좌표는 출발→서로 다른 중간점→도착이며 거리 배율은 1.0/1.15/1.3입니다. 시작·도착이 같거나 매우 가까우면 좌표 반올림에 따른 중복 필터로 반환 수가 줄 수 있습니다.

### 상태

- `normal`: 골든타임 만족 후보 ≥ 1. 후보가 1개여도 해당합니다.
- `partial`: 후보는 있으나 골든타임 만족 후보 0.
- `no_alternative`: 유효 후보 0. `routes: []`, `k_effective: 0`, `overlap_matrix: []`.

겹침은 소수점 5자리로 반올림한 **방향 있는 좌표 세그먼트**의 길이를 사용합니다. 공통 길이 / 합집합 길이로 계산하며 기하학적 교차나 서로 다른 분할의 동일 도로를 인식하는 map matching은 수행하지 않습니다. 원래 rank 오름차순 그리디 필터 → 골든타임 만족 여부·ETA 오름차순 정렬 → 1부터 rank 부여 → K 상한 적용 순서입니다. 100% 중복 후보만 있으면 첫 후보 1개가 남습니다.

### 오류

| HTTP | error.code | 조건 |
| --- | --- | --- |
| 400 | VALIDATION_ERROR | 필수값 누락, 좌표·선택값 범위 오류 |
| 400 | MALFORMED_REQUEST | 잘못된 JSON |
| 404 | NOT_FOUND | DB 및 mock fixture 모두에 없는 차량 |
| 502 | EXTERNAL_SYSTEM_ERROR | 실 Valhalla HTTP 오류, 10초 제한 초과, 잘못된 응답 |

오류 형식: `{"error":{"code":"NOT_FOUND","message":"...","request_id":null}}`.

### 실행과 검증

JDK 17, Gradle 배포본/의존성 다운로드 접근 및 로컬 MySQL이 필요합니다. 기본 DB는 `localhost:3306/firedispatch`, 계정은 `LOCAL_DB_USERNAME` / `LOCAL_DB_PASSWORD`(기본 root/root)입니다. 기존 Flyway V1~V4.1을 그대로 사용하고 새 마이그레이션은 추가하지 않습니다.

```sh
./gradlew clean test
./gradlew bootRun
# 다른 터미널에서 실행
curl -i http://localhost:8080/api/route \
  -H 'Content-Type: application/json' \
  -d '{"vehicle_id":"pump-3.5","from":{"lat":37.44,"lon":127.14},"to":{"lat":37.45,"lon":127.16}}'
```

`MockValhallaClient`는 `!prod` 프로필에서 활성화됩니다. **실 전환에는 `prod` 프로필과 `VALHALLA_URL` 둘 다 필요**하며, 환경변수만 설정해도 local이 실 경로로 바뀌지는 않습니다. prod에서는 기존 DB 환경변수도 필요합니다. Valhalla에 truck 제원(폭·높이·길이 m, 무게 ton, hazmat=false), exclude_polygons, alternates=k-1, kilometers를 전달합니다.

### 후속 연결

- staticdata 브랜치 머지 후 `MockNoGoLookup`을 실 `NoGoAreaService.forRouting()` 어댑터로 교체합니다. 현재는 500m 여유 bbox를 입력받아 빈 목록을 반환합니다.
- CCTV 파이프라인 연동 후 `passable_prob` placeholder를 실 조회 결과로 교체합니다.
- 설명은 `ExplanationBuilder` 인터페이스를 통해 추후 AI 구현체로 교체할 수 있습니다.

## 기존 mock API 목록

모든 JSON 필드는 snake_case입니다. 아래 API의 mock 동작은 유지합니다.

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/scenarios` | 화재 시나리오 3건 |
| GET | `/api/no_go` | 진입곤란 구간과 사유 (mock 아님, DB 조회) |
| GET | `/api/cctv/{id}` | CCTV 판독·유효 폭·confidence |
| GET | `/api/vehicles` | pump-3.5, pump-8 차량 제원 |
| GET | `/api/fire-system/nearest?lat=&lon=` | 외부 소방 시스템 mock 연계 |
| POST | `/api/incidents` | 신고 접수. 접수번호 발번 |
| GET | `/api/incidents?status=` | 신고 목록. 접수 최신순 |
| GET | `/api/incidents/{incident_no}` | 신고 상세 |
| POST | `/api/incidents/{incident_no}/routes` | 출동 경로 산출 + 판단 근거 저장 |
| GET | `/api/incidents/{incident_no}/routes` | 저장된 경로와 근거 |
| POST | `/api/incidents/{incident_no}/attachments` | 업로드한 사진·동영상을 신고에 첨부(존재·크기 확인) |
| GET | `/api/incidents/{incident_no}/attachments` | 신고 첨부 목록과 조회 URL |
| POST | `/api/files/upload-url` | 사진·동영상 업로드용 presigned URL 발급 |

`POST /api/route`는 위 라우팅 섹션의 새 계약(`vehicle_id`, `from`, `to`)을 사용합니다. 낡은 `scenario_id`/`lat`/`lon` 계약은 제거되었습니다.

## GET /api/no_go

중원구청 관내도에서 추출한 소방차 진입곤란 구간입니다. mock이 아니라 `no_go_areas` 테이블을 그대로 읽습니다.

**`bbox` 로 화면 범위만 받으세요.** 전건이 1,276건(약 320KB)이라 지도를 움직일 때마다 전부 받을
이유가 없습니다. 형식은 GeoJSON 과 같은 `minLon,minLat,maxLon,maxLat` 입니다 — 이 API 는
어디서나 lon 이 먼저입니다(응답의 `path` 도 그렇습니다).

```
GET /api/no_go?bbox=127.140,37.435,127.145,37.440
```

네 값이 아니거나, 숫자가 아니거나, 최소값이 최대값보다 크면 422 입니다.
`bbox` 를 빼면 전건을 내려줍니다. 두 경우 모두 `Cache-Control: max-age=300, public` 이 붙습니다 —
재임포트 전에는 바뀌지 않는 정적 데이터입니다.

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
## 인증 — X-Api-Token

신고 도메인과 업로드 URL 발급에는 공유 토큰이 필요합니다.

| 경로 | 토큰 |
| --- | --- |
| `/api/incidents`, `/api/incidents/**` | 필요 |
| `/api/files/upload-url` | 필요 |
| `/api/route`, `/api/no_go`, `/api/vehicles`, `/api/scenarios`, `/api/cctv` | 불필요 (공개 데모 데이터) |

```
X-Api-Token: <서버 환경변수 API_TOKEN 과 같은 값>
```

헤더가 없거나 값이 다르면 401 `UNAUTHORIZED` 입니다.

**서버에 `API_TOKEN` 이 비어 있으면 인증이 꺼진 채로 돕니다.** 프론트 BFF 가 헤더를 붙이기 전에
백엔드만 먼저 배포돼도 사이트가 죽지 않게 하려는 것입니다. 양쪽에 같은 값을 넣는 순간 켜집니다.
토큰은 BFF 가 서버에서 붙이므로 브라우저에는 노출되지 않습니다. 켜기 전까지는 기동할 때마다
로그에 경고가 남습니다.

## 파일 업로드

`POST /api/files/upload-url` 요청 예: `{ "content_type": "image/jpeg" }`.
허용 타입은 사진 `image/jpeg`, `image/png`, `image/webp` (최대 5MB) 와
동영상 `video/mp4`, `video/quicktime` (최대 50MB) 이고 그 밖은 422 입니다.

응답 예:

```json
{ "upload_url": "https://<bucket>.s3.ap-northeast-2.amazonaws.com/uploads/2026-09-09/<uuid>?X-Amz-...",
  "key": "uploads/2026-09-09/<uuid>",
  "expires_in_seconds": 300 }
```

클라이언트는 받은 `upload_url` 로 **직접 PUT** 합니다(파일 바이트는 백엔드를 거치지 않습니다).
이때 `Content-Type` 헤더는 발급 요청에 쓴 값과 같아야 합니다 — 서명 대상이라 다르면 S3가 403 입니다.

업로드가 끝나면 `key` 만 백엔드로 보냅니다. 지금 key 를 받는 곳은 **신고 첨부**
(`POST /api/incidents/{incident_no}/attachments`, 아래 참고)뿐입니다. 저장하는 쪽 도메인은 DB에 넣기 전에
`FileUploadService.confirmUpload(key)` 로 실제 존재와 크기를 확인해야 합니다 —
프론트의 "올렸어요" 보고만 믿으면 유령 레코드가 생깁니다.

업로드 상한은 파일 하나당 사진 5MB, 동영상 50MB 입니다. presigned PUT 자체는 크기를 막지 못하므로,
`confirmUpload()` 가 올라온 뒤 HEAD 로 재보고 초과분은 지운 다음 422 를 돌려줍니다.
`upload-url` 이 발급한 모양(`uploads/<날짜>/<uuid>`)이 아닌 key 도 422 입니다.
발급 API 남용은 Nginx rate limit(IP당 분당 5회)과 S3 수명주기 규칙(`uploads/` 30일 만료)이 함께 막습니다.

조회는 `FileUploadService.issueDownloadUrl(key)` 로 그때그때 만들어 내려줍니다.
DB에는 URL이 아니라 **key만** 저장합니다(버킷을 옮기거나 CloudFront를 붙여도 데이터가 안 썩습니다).

로컬(prod 프로파일이 아닐 때)은 S3 대신 `LocalStorageAdapter` 가 붙어 `build/local-storage/` 에
파일을 두고 `/local-storage/**` 로 서빙합니다. AWS 자격증명이 없어도 됩니다.

## 신고 접수

`POST /api/incidents` 요청 예: `{ "address": "성남시 중원구 은행로 12-3", "lat": 37.4381, "lon": 127.1422, "summary": "주택 화재" }`.

응답(201) 예:

```json
{ "incident_no": "2026-0915-0001", "status": "RECEIVED",
  "address": "성남시 중원구 은행로 12-3", "lat": 37.4381, "lon": 127.1422,
  "summary": "주택 화재", "received_at": "2026-09-15T14:30:00", "closed_at": null }
```

- 접수번호는 `연도-MMdd-일련번호` 입니다. 그날 이미 발급한 번호의 최댓값에서 이어 붙이므로,
  행이 지워져도 번호가 뒤로 돌아가지 않습니다. 동시 접수로 번호가 겹치면 유니크 키가 막고 다시 땁니다.
  재시도(5회)를 다 쓰면 409 입니다 — 서버 고장이 아니라 잠시 후 다시 보내면 되는 상황입니다.
- `status` 는 `RECEIVED` / `DISPATCHED` / `ON_SCENE` / `CLOSED` / `CANCELLED` 입니다.
  목록 조회의 `status` 파라미터는 대소문자를 가리지 않고, 없는 값이면 422 입니다.
- **좌표가 권역을 크게 벗어나면 422 입니다.** 관할 확인이 아니라 위경도를 뒤바꿔 넣은 실수를
  입구에서 잡기 위한 검사입니다(위도 자리에 127 이 오면 걸립니다). 인접 구 공조 출동은 막지 않습니다.
- 좌표는 `POINT` 가 아니라 `lat`/`lon` 으로 저장합니다. `scenarios` 와 같은 방식입니다.

## 출동 경로

`POST /api/incidents/{incident_no}/routes` 요청 예: `{ "vehicle_id": "pump-8", "from_lat": 37.4415, "from_lon": 127.1432 }`.
도착지는 신고 좌표라 받지 않습니다.

응답 예(권장 경로 1건 발췌):

```json
{ "rank": 1, "recommended": false, "vehicle_id": "pump-8",
  "distance_m": 1840, "eta_seconds": 420, "polyline": "...",
  "passable_for_vehicle": false, "meets_golden_time": true, "passable_prob": 0.9,
  "explanation": "진입곤란 2건으로 우회",
  "excluded_reasons": [
    { "polygon_id": "geumgwang1-impassable-003", "reason": "소방차 진입곤란 지정", "evidence_url": null }
  ],
  "unlocked_by_cctv": ["cctv-01"] }
```

- **`excluded_reasons` 가 이 기능의 존재 이유입니다.** 라우팅은 결과를 응답으로 한 번 뱉고 끝이라
  “왜 이 경로였는지”가 휘발됩니다. 안전 제품에서 판단 근거가 안 남으면 나중에 설명할 방법이 없습니다.
  `polygon_id` 는 `no_go_areas.ext_id` 입니다.
- `recommended` 는 **순위 1이면서 차량이 실제로 지날 수 있을 때만** true 입니다.
  1순위여도 `passable_for_vehicle` 이 false 면 권장이 아닙니다.
- `unlocked_by_cctv` 는 CCTV 판독으로 풀린 정적 진입곤란 구간입니다.
- **다시 호출하면 이전 결과를 대체합니다.** 차량이 바뀌면 판정이 통째로 바뀌기 때문입니다.
  산출 전에는 빈 목록을 돌려줍니다.

## 신고 첨부

사진·동영상은 3단계로 붙입니다.

1. `POST /api/files/upload-url` 로 `upload_url` 과 `key` 를 받는다(위 "파일 업로드").
2. `upload_url` 로 파일을 **직접 PUT** 한다. `Content-Type` 은 1에서 보낸 값과 같아야 한다.
3. `POST /api/incidents/{incident_no}/attachments` 에 `{ "key": "uploads/2026-09-16/<uuid>" }` 를 보낸다.

3의 응답(201) 예:

```json
{ "key": "uploads/2026-09-16/<uuid>", "content_type": "video/mp4", "size_bytes": 18234567,
  "download_url": "https://<bucket>.s3.ap-northeast-2.amazonaws.com/uploads/2026-09-16/<uuid>?X-Amz-...",
  "created_at": "2026-09-16T19:40:00" }
```

- 3에서 백엔드가 S3 에 실제로 있는지, 크기가 상한 안인지 확인합니다. 올리지 않았거나 상한을 넘으면 422 이고,
  **상한을 넘은 파일은 S3에서 지워집니다.** 2에서 PUT 이 끝나기 전에 3을 부르면 422 입니다.
- `content_type` · `size_bytes` 는 프론트가 보낸 값이 아니라 S3 에서 잰 값입니다.
- 같은 `key` 를 다시 붙이면(다른 신고여도) 409 `CONFLICT` 입니다. 없는 접수번호는 404 입니다.
- `GET /api/incidents/{incident_no}/attachments` 는 붙인 순서대로 돌려줍니다. 없으면 빈 목록입니다.
- **`download_url` 은 10분이면 만료됩니다**(`presign-get-ttl`). 저장해 두지 말고 화면을 열 때마다 목록을 다시 부르세요.
