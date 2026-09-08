# Golden Lane Backend
성남 구도심 소방차 진입 최적화용 Spring Boot 4.1.1 백엔드입니다.

## Setup
Java 21, Docker가 필요합니다. `.env.example`을 참고해 환경 변수를 설정합니다.
```bash
make db-up
make run
```
API는 http://localhost:8080, Swagger는 http://localhost:8080/swagger-ui.html 입니다.

## Test
```bash
make test
make build
```
통합 실행은 MySQL 8.4 컨테이너를 사용합니다. Hibernate DDL은 `validate`이며 스키마 변경은 Flyway로만 합니다.

## 담당자
| 모듈 | 담당 |
|---|---|
| routing | 박종준 |
| staticdata/no_go | 이태연 |
| cctv | 유강현 |
| vehicles | 윤종호 |
| scenarios | 공용 |

CI/CD는 윤종호 담당 범위로 이 초기 세팅에서 제외했습니다.

