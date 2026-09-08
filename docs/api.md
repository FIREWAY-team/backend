# API
- GET /api/scenarios: 사전 정의 시나리오 목록
- POST /api/route: body `{"scenarioId":"scenario-001","vehicleId":"pump-3.5"}`, 경로 3안
- GET /api/vehicles: 차량 제원
- GET /api/no_go?bbox=minLon,minLat,maxLon,maxLat&layer=1: GeoJSON FeatureCollection
- GET /api/cctv/{cctvId}: 스틸 URL, 잔여 폭, verdict
- GET /actuator/health: UptimeRobot health check
Swagger UI: `/swagger-ui.html`

