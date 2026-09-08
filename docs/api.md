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

`POST /api/route` 요청 예: `{ "scenario_id": "bank-01", "lat": 37.4381, "lon": 127.1422, "vehicle_id": "pump-3.5" }`.
