#!/usr/bin/env bash
# 시연용 신고 3건을 접수한다. 좌표는 /api/scenarios 의 bank-01 · sangdaewon-01 · moran-01 과 같다.
# 시연 데이터를 Flyway 로 박지 않는 이유: 녹화를 다시 할 때마다 새로 접수하면 그만이고,
# 지난 건은 PATCH /status 로 CLOSED 처리하면 상황실에서 빠진다.
#
#   BASE_URL=http://localhost:8080 API_TOKEN=... scripts/seed_demo_incidents.sh
set -euo pipefail
BASE_URL=${BASE_URL:-http://localhost:8080}

post() {
  curl -fsS -X POST "$BASE_URL/api/incidents" -H 'Content-Type: application/json' \
       ${API_TOKEN:+-H "X-Api-Token: $API_TOKEN"} -d "$1"
  echo
}

post '{"address":"성남시 중원구 은행동 일대","lat":37.4381,"lon":127.1422,"summary":"은행1동 화재",
  "reporter_name":"조OO (신고자)","reporter_phone":"010-****-5245","severity":"large","estimated_area_m2":242,
  "building_type":"4층 오피스텔","casualties_reported":true,"notes":"가연물 다량 · 골목 진입 어려움"}'
post '{"address":"성남시 중원구 상대원동 일대","lat":37.4311,"lon":127.1642,"summary":"상대원1동 화재",
  "reporter_name":"박OO (상가 점주)","reporter_phone":"010-****-3187","severity":"medium","estimated_area_m2":85,
  "building_type":"5층 상가 (1층 음식점)","casualties_reported":false,"notes":"튀김기 발화 · 초기 진화 시도 중"}'
post '{"address":"성남시 중원구 성남동 모란시장 일대","lat":37.4324,"lon":127.1299,"summary":"모란시장 화재",
  "reporter_name":"이OO (인근 상인)","reporter_phone":"010-****-2914","severity":"medium","estimated_area_m2":45,
  "building_type":"1층 상가 (기름집)","casualties_reported":false,"notes":"가연물 다량 · 폭 2.6m 골목 진입 · 좌우 적치물"}'
