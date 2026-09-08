# 아키텍처

Golden Lane은 기존 소방 지휘·신고 시스템에 얹히는 판단 엔진입니다. `src/modules` 아래 도메인별 모듈은 `domain → application → infrastructure → interfaces`의 Modular Clean Architecture를 따릅니다. 도메인은 프레임워크와 분리하고, 외부 시스템은 포트(Protocol)와 어댑터로 격리합니다. 현재 외부 연동은 `shared/external/fire_system.py`의 mock만 제공합니다.

| 모듈 | 담당 | 책임 |
|---|---|---|
| routing | 박종준 (jongjunn) | 경로 최적화, `/route` |
| static_data | 이태연 | PDF→GeoJSON, `/no_go` |
| cctv | 유강현 | CCTV 판독 결과, `/cctv/{edge_id}` |
| vehicles | 윤종호 | 차량 제원·커버리지 |
| scenarios | 공용/박종준 | 시연 시나리오 |

