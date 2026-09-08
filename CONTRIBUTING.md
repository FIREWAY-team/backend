# Contributing

브랜치는 `feature/<domain>-<short-name>` 또는 `fix/<short-name>` 형식을 사용합니다. 커밋은 현재형의 작은 단위로 작성하고, PR에는 목적·테스트·마이그레이션 여부를 적습니다. PR은 리뷰 후 `main`에 squash merge합니다.

## Flyway 마이그레이션 규칙

인원별 major version 배정 (건드리지 말 것):

| Major | 담당 | 도메인 |
|---|---|---|
| V1 | 박종준 | scenarios, routing |
| V2 | 이태연 | no_go_areas |
| V3 | 유강현 | cctv_readings |
| V4 | 윤종호 | vehicles |

같은 major 안에서 새 변경은 소수점 서브버전으로:
- `V1__init_scenarios.sql`
- `V1_1__scenarios_fixture.sql` ← Flyway가 V1.1로 파싱
- `V1_2__add_route_metadata_column.sql`

⚠️ Flyway는 파일 알파벳순이 아니라 semver 순으로 실행: `V1 → V1.1 → V1.2 → V2 → V2.1 → V3 → V4`

**다른 사람의 major version은 절대 건드리지 말 것.** 도메인 간 FK가 필요하면 팀 채팅에서 협의.

**주의**: 이미 프로덕션에 적용된 마이그레이션 파일은 절대 수정하지 말 것. 수정이 필요하면 새 서브버전으로 추가합니다.
