#!/usr/bin/env bash
#
# 블루-그린 전환 (한 번만 돌린다).
#
# 단일 backend 컨테이너(127.0.0.1:8080) 구성을
# backend-blue(8081) / backend-green(8082) + nginx upstream 구성으로 바꾼다.
#
# 이 전환 자체도 다운타임 없이 간다.
#   blue 를 먼저 띄우고 -> health 확인 -> nginx 를 blue 로 돌리고 -> 옛 컨테이너를 내린다
#
# 실행:
#   sudo bash /home/ubuntu/deploy/backend/deploy/setup-bluegreen.sh
#
# 중간에 실패하면 nginx 설정과 compose 를 원래대로 되돌리고 끝난다.

set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/home/ubuntu/deploy}"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.yml"
REPO_COMPOSE="${REPO_COMPOSE:-$DEPLOY_DIR/backend/deploy/docker-compose.prod.yml}"
UPSTREAM_CONF="/etc/nginx/conf.d/backend-upstream.conf"
NGINX_DIRS="/etc/nginx/sites-enabled /etc/nginx/conf.d"
BLUE_PORT=8081
STAMP="$(date +%Y%m%d-%H%M%S)"

[ "$(id -u)" -eq 0 ] || { echo "root 로 실행할 것: sudo bash $0" >&2; exit 1; }
[ -f "$REPO_COMPOSE" ] || { echo "$REPO_COMPOSE 가 없다. 리포가 최신인지 확인할 것." >&2; exit 1; }

echo "=== 0. 스왑 확인 ==="
if swapon --show | grep -q .; then
  echo "  스왑 있음: $(swapon --show --noheadings --raw | head -1)"
else
  echo "  스왑 없음 -> 2G 만든다"
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo "  스왑 2G 추가 완료"
fi
free -m

echo "=== 1. 백업 ==="
cp -a "$COMPOSE_FILE" "$COMPOSE_FILE.bak.$STAMP"
tar czf "/root/nginx-backup-$STAMP.tar.gz" /etc/nginx 2>/dev/null
echo "  compose  -> $COMPOSE_FILE.bak.$STAMP"
echo "  nginx    -> /root/nginx-backup-$STAMP.tar.gz"

rollback() {
  echo "!!! 실패 -> 되돌린다" >&2
  cp -a "$COMPOSE_FILE.bak.$STAMP" "$COMPOSE_FILE" 2>/dev/null || true
  rm -f "$UPSTREAM_CONF"
  tar xzf "/root/nginx-backup-$STAMP.tar.gz" -C / 2>/dev/null || true
  nginx -t >/dev/null 2>&1 && systemctl reload nginx || true
  docker rm -f backend-blue >/dev/null 2>&1 || true
  echo "되돌렸다. 기존 backend 컨테이너와 nginx 설정이 그대로다." >&2
  exit 1
}
trap rollback ERR

echo "=== 2. compose 를 블루-그린 버전으로 교체 ==="
cp -a "$REPO_COMPOSE" "$COMPOSE_FILE"

echo "=== 3. backend-blue 기동 (옛 backend 는 계속 서비스 중) ==="
cd "$DEPLOY_DIR"
docker compose pull backend-blue
docker compose up -d backend-blue

echo "=== 4. backend-blue 헬스체크 ==="
ok=no
for i in $(seq 1 45); do
  if curl -fsS --max-time 3 "http://127.0.0.1:$BLUE_PORT/actuator/health" 2>/dev/null \
       | grep -q '"status":"UP"'; then
    echo "  UP (${i}번째 시도)"; ok=yes; break
  fi
  sleep 2
done
[ "$ok" = yes ] || { docker compose logs --tail 80 backend-blue >&2; false; }

echo "=== 5. nginx upstream 추가 + proxy_pass 교체 ==="
printf 'upstream backend_active {\n    server 127.0.0.1:%s;\n}\n' "$BLUE_PORT" > "$UPSTREAM_CONF"

# 백엔드로 가던 proxy_pass 만 바꾼다. 프론트(3000)는 건드리지 않는다.
HITS=$(grep -rl 'proxy_pass http://127\.0\.0\.1:8080' $NGINX_DIRS 2>/dev/null || true)
[ -n "$HITS" ] || { echo "  proxy_pass 8080 을 못 찾았다" >&2; false; }
echo "$HITS" | while read -r f; do
  [ -n "$f" ] || continue
  sed -i 's|proxy_pass http://127\.0\.0\.1:8080|proxy_pass http://backend_active|g' "$f"
  echo "  고침: $f"
done

nginx -t
systemctl reload nginx
echo "  reload 완료"

echo "=== 6. 외부에서 확인 ==="
curl -fsS --max-time 5 https://fireroad.shop/actuator/health | grep -q '"status":"UP"' \
  || { echo "  외부 헬스체크 실패" >&2; false; }
echo "  https://fireroad.shop/actuator/health UP"

echo "=== 7. 옛 backend 컨테이너 정리 ==="
docker rm -f backend >/dev/null 2>&1 || true
docker image prune -f >/dev/null

trap - ERR
echo
echo "전환 완료. 이제부터 배포는 blue <-> green 으로 돈다."
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
free -m
