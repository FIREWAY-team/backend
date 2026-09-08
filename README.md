# Golden Lane Backend

성남 구도심 좁은 골목의 소방차 진입 가능성을 판단하는 기존 소방 시스템 하위 엔진입니다. 예선 범위는 사전 판독 CCTV 결과, 정적 진입불가 데이터, 라우팅이며 실시간 처리는 포함하지 않습니다.

## 시작하기

Python 3.12+와 [uv](https://docs.astral.sh/uv/)를 설치한 뒤:

```bash
cp .env.example .env
uv sync
make dev
```

DB까지 필요하면 `make db-up` 후 `make migrate`를 실행합니다. `HEALTH_CHECK_DB=false`이면 로컬 DB 없이도 API 스모크 실행이 가능합니다.

```bash
make test
make lint
make format
docker compose up --build
```

## 구조와 담당

각 모듈은 `domain`, `application`, `infrastructure`, `interfaces`의 Hexagonal 경계를 가집니다. 상세한 경계는 [docs/architecture.md](docs/architecture.md), API는 [docs/api.md](docs/api.md)를 참고하세요.

| 담당 | 모듈 | 다음 구현 |
|---|---|---|
| 박종준 (jongjunn) | routing | 네트워크 모델·최적화 유스케이스·Valhalla 포트 |
| 이태연 | static_data | PDF 파싱·GeoJSON 적재·bbox/layer 조회 |
| 유강현 | cctv | 스틸 저장소·판독 결과 모델·edge 조회 |
| 윤종호 | vehicles | 차량 제원·커버리지 계산 및 영속화 |
| 공용/박종준 | scenarios | 시나리오 모델·fixture/조회 유스케이스 |

## API 확인

실행 후 `/health`, `/docs`, `/openapi.json`에서 확인할 수 있고, 현재 모든 도메인 라우터는 계약 검증용 mock JSON을 반환합니다. 공통 예외 응답은 `{ "error": { "code", "message", "request_id" } }`입니다.

## 개발 규칙

브랜치와 커밋 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 따릅니다. CI/CD 워크플로는 이 초기 세팅에 포함하지 않습니다.
