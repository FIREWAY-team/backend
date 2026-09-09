# feat(routing): 전 경로 탐색 API + 5분 골든타임 우선 정렬

## 요약
관할 소방서 → 화점 앞 정차점까지의 소방차 경로 3안을 차량 제원·정적 진입곤란·(추후) CCTV 판독과 결합해 계산하고, **5분(300초) 골든타임 만족 후보를 상위로 정렬**해 상황실에 제시합니다.

## 변경 사항
- `modules/routing` 실구현 (기존 mock HTTP 계약 교체, 레거시 모델·서비스·저장소 파일 보존)
- `POST /api/route` 계약 확정 — snake_case, `routes[]` 배열, `alternatives_status` 명시
- `MockValhallaClient` 기본 (`!prod`), `RealValhallaClient`는 `prod` 프로필 + `VALHALLA_URL` 환경변수로 활성
- 실 Valhalla WebClient 요청에 truck 제원·exclude_polygons·alternates 전달, polyline6 해석 및 미터 환산, 외부 오류 502 처리
- `VehicleService.findById(String)` 신규 (MySQL vehicles 조회 우선, 행이 없으면 기존 mock fixture 검색)
- `WeightedOverlapCalculator` (좌표 세그먼트 기반, 임계 0.65, 앞선 rank부터 그리디 필터)
- `GoldenTimePrioritizer` (기본 SLA 300초, `meets_golden_time` 우선·ETA 오름차순 정렬)
- `RuleBasedExplanationBuilder` (Claude 미연결 시 폴백)
- 신규 마이그레이션 없음. 기존 V4/V4.1 vehicles 스키마·fixture 사용

## 계약 변경 (프론트 확인 요청)
- **breaking**: `POST /api/route` 요청·응답 구조가 완전히 변경됨 (mock 스켈레톤 → 실 계약)
- 기존 `scenario_id`, `lat`, `lon` 요청 대신 `vehicle_id`, `from`, `to` 필요
- routing 입력 검증 실패는 400. 다른 모듈의 공통 422 계약은 유지
- 상세: [docs/api.md](api.md). 이미 존재하던 문서를 갱신하고 기존 다른 API 목록 보존
- 기본 mock의 `passable_prob=1.0`은 CCTV 연동 전 placeholder

## 종속성
- **이태연 `feature/staticdata-nogo-import`** 머지 후 `MockNoGoLookup`을 실 `NoGoAreaService.forRouting()`으로 교체 (별도 PR)
- **유강현 `ai` 리포** 파이프라인 완료 후 `cctv_readings` 조회 붙임 (별도 PR)
- 실 Valhalla tile 성남 빌드 후 mock → real 스위치 (별도 PR)

## 테스트
- 신규 JUnit 테스트 **23개 작성**: routing 21개, vehicles 2개. 현재 환경에서는 Gradle 다운로드 DNS 실패로 **실행·통과 여부 미확인**.
- 브리프의 핵심 15개 검증과 추가 8개 경계/외부 응답 검증. 기존 smoke test는 변경된 routing 계약으로 갱신.
- 기존 `GlobalExceptionHandlerTest` 중복 `mvc` 필드 컴파일 오류 수정 및 중첩 테스트 controller 명시적 등록.
- `./gradlew clean test`, `./gradlew test`: exit 1 — `UnknownHostException: services.gradle.org` (Gradle 8.14.5 다운로드 단계).
- `./gradlew bootRun`: 동일 다운로드 실패. 이후 curl은 exit 7 (localhost:8080 연결 실패).
- 의존성 없는 실제 `PolylineCodec`는 JDK 17로 직접 컴파일하고 알려진 polyline6 값·음수/경계 좌표 왕복·잘못된 입력 거부 검증 통과.
- DB 및 remote 경계는 Planner SpringBootTest에서 mock 처리. 실제 MySQL 조회/Flyway와 Valhalla 서버 연동 검증은 미완료.

## 브리프 모순 처리
브리프 5장의 그리디 필터는 첫 후보를 반드시 보존하므로, 9장의 “100% overlap 후보를 모두 제거해 no_alternative” 테스트 조건은 성립하지 않습니다. 필터 규칙을 유지하고 **빈 후보 → no_alternative** 및 **100% 중복 → 첫 후보 1개 유지**로 나누어 검증합니다. custom SLA 설명은 고정된 5분 대신 실제 요청 초를 표시합니다.

## 남은 검증
- 네트워크 가능한 JDK 17 환경에서 `./gradlew clean test` 통과 확인
- 로컬 MySQL 준비 후 bootRun 및 curl 200/400/404, 기본·custom SLA/K 검증
- 위 검증 완료 후 이 테스트 결과를 실제 통과 결과로 갱신

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
🤖 Generated with [Claude Code](https://claude.com/claude-code)
