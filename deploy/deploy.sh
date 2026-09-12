#!/usr/bin/env bash
#
# fireroad 백엔드 무중단 배포 (블루-그린).
#
# GitHub Actions 의 deploy 잡이 SSM send-command 로 이 스크립트를 실행한다.
# SSH 가 아니라 SSM 인 이유: SSH 는 집 IP 로 제한돼 있어 러너가 못 들어온다.
#
# 흐름
#   1. 지금 안 쓰는 색(idle)으로 새 이미지를 띄운다
#   2. 그 색이 health UP 이 될 때까지 기다린다 (현재 색은 계속 서비스 중)
#   3. nginx upstream 을 새 색으로 바꾸고 reload 한다
#      reload 는 기존 연결을 끊지 않으므로 요청이 안 끊긴다
#   4. 옛 색을 내린다
#
# 어디서 실패하든 현재 서비스 중인 색은 건드리지 않는다.
# 실패하면 새로 띄운 쪽만 정리하고 끝난다. 다운타임이 없다.
#
# 주의: 서버에서 docker build 를 돌리지 말 것.
#       램이 1GB(t3.micro)뿐이라 다른 컨테이너가 OOM 으로 죽는다.

set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/home/ubuntu/deploy}"
COMPOSE_FILE="${COMPOSE_FILE:-$DEPLOY_DIR/docker-compose.yml}"
UPSTREAM_CONF="${UPSTREAM_CONF:-/etc/nginx/conf.d/backend-upstream.conf}"

BLUE_PORT="${BLUE_PORT:-8081}"
GREEN_PORT="${GREEN_PORT:-8082}"

# 기동에 보통 20초쯤 걸린다. 두 배 넘게 잡아둔다.
HEALTH_RETRIES="${HEALTH_RETRIES:-45}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-2}"

dc() { docker compose -f "$COMPOSE_FILE" "$@"; }

log() { echo "$*"; }

# ------------------------------------------------------------------
# 사전 점검 — 여기서 걸리면 아무것도 건드리지 않고 끝난다
# ------------------------------------------------------------------
command -v curl >/dev/null 2>&1 || { echo "curl 이 없다" >&2; exit 1; }

sudo -n true 2>/dev/null || {
  echo "비밀번호 없는 sudo 가 필요하다 (nginx reload)." >&2
  exit 1
}

[ -f "$UPSTREAM_CONF" ] || {
  echo "$UPSTREAM_CONF 가 없다." >&2
  echo "서버에서 deploy/setup-bluegreen.sh 를 한 번 돌려 전환을 끝낸 뒤 다시 배포할 것." >&2
  exit 1
}

cd "$DEPLOY_DIR"

# ------------------------------------------------------------------
# 지금 어느 색이 서비스 중인가 — nginx upstream 이 정답이다
# ------------------------------------------------------------------
if grep -q ":${BLUE_PORT};" "$UPSTREAM_CONF"; then
  ACTIVE=blue;  IDLE=green; IDLE_PORT="$GREEN_PORT"
else
  ACTIVE=green; IDLE=blue;  IDLE_PORT="$BLUE_PORT"
fi
log "현재 서비스 중: $ACTIVE / 새로 띄울 쪽: $IDLE (127.0.0.1:$IDLE_PORT)"

# 실패하면 새로 띄운 쪽만 치운다. 서비스 중인 색은 그대로 둔다.
cleanup_idle() {
  log "  실패 정리: backend-$IDLE 내림 (backend-$ACTIVE 는 계속 서비스 중)"
  dc stop "backend-$IDLE" >/dev/null 2>&1 || true
  dc rm -f "backend-$IDLE" >/dev/null 2>&1 || true
}

# ------------------------------------------------------------------
log "[1/5] 새 이미지 받기"
dc pull "backend-$IDLE"

log "[2/5] backend-$IDLE 기동"
# 혹시 이전 배포가 중간에 죽어 남아 있으면 먼저 치운다
dc rm -f "backend-$IDLE" >/dev/null 2>&1 || true
dc up -d "backend-$IDLE"

log "[3/5] backend-$IDLE 헬스체크"
ok=no
for i in $(seq 1 "$HEALTH_RETRIES"); do
  if curl -fsS --max-time 3 "http://127.0.0.1:${IDLE_PORT}/actuator/health" 2>/dev/null \
       | grep -q '"status":"UP"'; then
    log "      UP (${i}번째 시도)"
    ok=yes
    break
  fi
  sleep "$HEALTH_INTERVAL"
done

if [ "$ok" != yes ]; then
  echo "      헬스체크 실패. 트래픽은 backend-$ACTIVE 에 그대로 있다." >&2
  echo "      backend-$IDLE 로그:" >&2
  dc logs --tail 80 "backend-$IDLE" >&2 || true
  cleanup_idle
  exit 1
fi

# ------------------------------------------------------------------
log "[4/5] nginx 를 backend-$IDLE 로 전환"
BACKUP="$(mktemp)"
sudo cat "$UPSTREAM_CONF" > "$BACKUP"

printf 'upstream backend_active {\n    server 127.0.0.1:%s;\n}\n' "$IDLE_PORT" \
  | sudo tee "$UPSTREAM_CONF" >/dev/null

if ! sudo nginx -t >/dev/null 2>&1; then
  echo "      nginx 설정 검사 실패. 되돌린다." >&2
  sudo tee "$UPSTREAM_CONF" < "$BACKUP" >/dev/null
  sudo nginx -t >&2 || true
  rm -f "$BACKUP"
  cleanup_idle
  exit 1
fi

# reload 는 기존 연결을 끊지 않는다. 여기서 다운타임이 생기지 않는다.
sudo systemctl reload nginx

# 진짜로 nginx 를 통해 새 쪽이 응답하는지 확인한다
if ! curl -fsS --max-time 5 https://fireroad.shop/actuator/health 2>/dev/null \
     | grep -q '"status":"UP"'; then
  echo "      전환 후 외부 헬스체크 실패. upstream 을 되돌린다." >&2
  sudo tee "$UPSTREAM_CONF" < "$BACKUP" >/dev/null
  sudo nginx -t >/dev/null 2>&1 && sudo systemctl reload nginx
  rm -f "$BACKUP"
  cleanup_idle
  exit 1
fi
rm -f "$BACKUP"
log "      전환 완료. 외부 트래픽이 backend-$IDLE 로 간다"

# ------------------------------------------------------------------
log "[5/5] backend-$ACTIVE 내리고 정리"
# 여기서야 옛 컨테이너를 내린다. 이 시점엔 트래픽이 이미 새 쪽으로 갔다.
# frontend 는 네트워크 별칭 backend 로 붙는데, 옛 컨테이너가 빠지면
# 별칭은 새 쪽만 가리킨다.
dc stop "backend-$ACTIVE" >/dev/null 2>&1 || true
dc rm -f "backend-$ACTIVE" >/dev/null 2>&1 || true

docker image prune -f >/dev/null

log "배포 완료: backend-$IDLE (127.0.0.1:$IDLE_PORT) $(dc images "backend-$IDLE" --quiet 2>/dev/null | head -c 12)"
