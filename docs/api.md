# API — Routing

## POST /api/route

전 경로 탐색: 관할 소방서 → 화점 앞 정차점까지 차량 제원·정적 진입곤란·(추후) CCTV 판독을 결합해 최대 3개 후보를 반환합니다. **5분(300초) 골든타임 만족 후보를 상위로 정렬**합니다.

현재 기본 프로필에서는 mock 경로를 제공합니다. 실제 도로망·진입곤란·CCTV에 기반한 운행 판단은 후속 연동 후 가능합니다.

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
| GET | `/api/no_go` | No-Go 폴리곤과 사유 |
| GET | `/api/cctv/{id}` | CCTV 판독·유효 폭·confidence |
| GET | `/api/vehicles` | pump-3.5, pump-8 차량 제원 |
| GET | `/api/fire-system/nearest?lat=&lon=` | 외부 소방 시스템 mock 연계 |
