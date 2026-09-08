# API v2

FastAPI가 Pydantic v2 스키마와 OpenAPI를 자동 생성합니다 (`/docs`, `/openapi.json`).

| Method | Path | 설명 |
|---|---|---|
| GET | `/health` | 서비스 상태 |
| GET | `/scenarios` | 시연 시나리오 목록 |
| POST | `/route` | origin/destination/vehicle_id 기반 모의 경로와 진입가능성 확률 |
| GET | `/vehicles` | 차량 제원 |
| GET | `/coverage?vehicle_id=xxx` | 차량별 커버리지 |
| GET | `/no_go?bbox=...&layer=1,3` | 진입불가 GeoJSON |
| GET | `/cctv/{edge_id}` | CCTV 스틸과 판독 결과 |

오류는 `{ "error": { "code", "message", "request_id" } }` 형식이며 모든 응답에 `X-Request-ID`가 포함됩니다.

