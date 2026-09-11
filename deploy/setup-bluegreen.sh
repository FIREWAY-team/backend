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
# 이 스크립트는 root 로 돌아야 한다(스왑/nginx). 하지만 docker 레지스트리
# 자격증명은 계정별(~/.docker/config.json)이고 GHCR 로그인은 ubuntu 에만 있다.
# 그래서 docker compose 만 ubuntu 로 돌린다. 배포(deploy.sh)도 ubuntu 로 돈다.
APP_USER="${APP_USER:-ubuntu}"
NGINX_DIRS="/etc/nginx/sites-enabled /etc/nginx/conf.d"
BLUE_PORT=8081
STAMP="$(date +%Y%m%d-%H%M%S)"

[ "$(id -u)" -eq 0 ] || { echo "root 로 실행할 것: sudo bash $0" >&2; exit 1; }
[ -f "$REPO_COMPOSE" ] || { echo "$REPO_COMPOSE 가 없다. 리포가 최신인지 확인할 것." >&2; exit 1; }
id "$APP_USER" >/dev/null 2>&1 || { echo "$APP_USER 계정이 없다" >&2; exit 1; }

# ubuntu 로 도는 docker compose
dcu() { sudo -u "$APP_USER" docker compose -f "$COMPOSE_FILE" "$@"; }

# GHCR 로그인이 ubuntu 에 있는지 미리 본다. 없으면 pull 이 unauthorized 로 죽는다.
if ! sudo -u "$APP_USER" test -s "$(getent passwd "$APP_USER" | cut -d: -f6)/.docker/config.json"; then
  echo "경고: $APP_USER 에 docker 자격증명이 안 보인다." >&2
  echo "      GHCR 이미지가 private 이면 pull 이 unauthorized 로 실패한다." >&2
  echo "      그 경우: sudo -u $APP_USER docker login ghcr.io" >&2
fi

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
dcu pull backend-blue
dcu up -d backend-blue

echo "=== 4. backend-blue 헬스체크 ==="
ok=no
for i in $(seq 1 45); do
  if curl -fsS --max-time 3 "http://127.0.0.1:$BLUE_PORT/actuator/health" 2>/dev/null \
       | grep -q '"status":"UP"'; then
    echo "  UP (${i}번째 시도)"; ok=yes; break
  fi
  sleep 2
done
[ "$ok" = yes ] || { dcu logs --tail 80 backend-blue >&2; false; }

echo "=== 5. nginx upstream 추가 + proxy_pass 교체 ==="
printf 'upstream backend_active {\n    server 127.0.0.1:%s;\n}\n' "$BLUE_PORT" > "$UPSTREAM_CONF"

# 백엔드로 가던 proxy_pass 만 바꾼다. 프론트(3000)는 건드리지 않는다.
#
# 어느 파일을 고칠지는 nginx 자신에게 묻는다. nginx -T 가 실제로 읽는
# 설정 파일 경로를 전부 찍어준다. sites-enabled 를 grep -r 로 뒤지면
# 심볼릭 링크를 안 따라가서 놓친다.
CONF_FILES="$(nginx -T 2>/dev/null | sed -n 's|^# configuration file \(.*\):$|\1|p' | sort -u)"
[ -n "$CONF_FILES" ] || CONF_FILES="$(find $NGINX_DIRS -type f -o -type l 2>/dev/null)"

# sed -i 를 심볼릭 링크에 걸면 링크가 일반 파일로 바뀐다.
# readlink -f 로 실체 경로를 구하고 중복을 없앤다.
HITS=""
for f in $CONF_FILES; do
  [ -r "$f" ] || continue
  if grep -q 'proxy_pass http://127\.0\.0\.1:8080' "$f" 2>/dev/null; then
    HITS="$HITS$(readlink -f "$f")\n"
  fi
done
HITS="$(printf '%b' "$HITS" | sed '/^$/d' | sort -u)"

if [ -z "$HITS" ]; then
  echo "  proxy_pass http://127.0.0.1:8080 을 어느 설정 파일에서도 못 찾았다" >&2
  echo "  nginx 가 읽는 파일들:" >&2
  printf '%s\n' $CONF_FILES | sed 's|^|    |' >&2
  echo "  실제 proxy_pass 줄:" >&2
  nginx -T 2>/dev/null | grep -n 'proxy_pass' | sed 's|^|    |' >&2
  false
fi

for f in $HITS; do
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
