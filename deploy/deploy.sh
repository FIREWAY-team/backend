#!/usr/bin/env bash
#
# fireroad 백엔드 배포 스크립트.
#
# GitHub Actions의 deploy 잡이 SSM send-command로 이 스크립트를 실행한다.
# SSH가 아니라 SSM인 이유: SSH는 집 IP로 제한돼 있어 Actions 러너가 못 들어온다.
#
# 주의: 서버에서 docker build 를 돌리지 말 것.
#       램이 1GB(t3.micro)뿐이라 다른 컨테이너가 OOM으로 죽는다.
#       이미지는 항상 Actions에서 만들어 GHCR에 올린다.

set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/home/ubuntu/deploy}"
COMPOSE_FILE="${COMPOSE_FILE:-$DEPLOY_DIR/docker-compose.yml}"
SERVICE="${SERVICE:-backend}"

# 백엔드는 127.0.0.1 에만 바인딩돼 있다. 헬스체크는 서버 안에서만 가능하다.
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"
HEALTH_RETRIES="${HEALTH_RETRIES:-30}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-2}"

cd "$DEPLOY_DIR"

echo "[1/4] GHCR에서 이미지 받기"
docker compose -f "$COMPOSE_FILE" pull "$SERVICE"

echo "[2/4] 컨테이너 교체"
# 지금은 무중단이 아니다. 기존 컨테이너를 내리고 새로 띄우므로
# 이 지점에서 수 초간 502가 난다. 무중단 전환은 별도 작업.
docker compose -f "$COMPOSE_FILE" up -d "$SERVICE"

echo "[3/4] 헬스체크"
for i in $(seq 1 "$HEALTH_RETRIES"); do
  if curl -fsS --max-time 3 "$HEALTH_URL" 2>/dev/null | grep -q '"status":"UP"'; then
    echo "      UP (${i}번째 시도)"
    break
  fi
  if [ "$i" -eq "$HEALTH_RETRIES" ]; then
    echo "      헬스체크 실패 — 배포를 실패로 처리한다. 최근 로그:" >&2
    docker compose -f "$COMPOSE_FILE" logs --tail 80 "$SERVICE" >&2
    exit 1
  fi
  sleep "$HEALTH_INTERVAL"
done

echo "[4/4] 떠 있는 이미지 정리"
# dangling 이미지만 지운다. 디스크가 작아서 안 하면 금방 찬다.
docker image prune -f

echo "배포 완료: $(docker compose -f "$COMPOSE_FILE" images "$SERVICE" --quiet | head -c 12)"
