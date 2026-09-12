# 배포

서버에만 있던 배포 파일들을 리포로 옮긴 것이다.
`deploy.sh` 는 여기 있는 것이 정본이고, 서버는 매 배포마다 이걸 받아서 쓴다.

## 왜 옮겼나

전에는 `deploy.sh` 와 운영 `docker-compose.yml` 이 EC2 의 `~/deploy/` 에만 있었다.

- 버전 관리가 안 됐다. 누가 언제 뭘 바꿨는지 기록이 없다
- 리뷰를 못 했다. 배포 방식 변경이 PR 에 안 잡혔다
- 서버가 날아가면 배포 방식도 같이 사라졌다

## 파일

| 파일 | 서버 위치 | 정본인가 |
|---|---|---|
| `deploy.sh` | `~/deploy/backend/deploy/deploy.sh` | **그렇다.** 배포가 이걸 실행한다 |
| `setup-bluegreen.sh` | 같음 | 한 번만 돌리는 전환 스크립트 |
| `docker-compose.prod.yml` | `~/deploy/docker-compose.yml` | 아니다. 서버 파일의 거울 |

서버에만 있고 리포에 없는 것:

- **`~/deploy/backend.env`** — 실제 DB 비밀번호가 들어 있다. 깃에 절대 넣지
  않는다 (`chmod 600`). 형식은 리포 루트의 `backend.env.example` 참고
- **`~/deploy/deploy-frontend.sh`** — 프론트 배포 스크립트. 이 리포 소관이
  아니라 옮기지 않았다. 프론트 리포로 옮기는 게 맞다

## 배포가 도는 방식

`main` / `develop` 에 머지되면 워크플로가 이 순서로 돈다.

```
test  ->  build-and-push (GHCR)  ->  deploy (SSM)
```

`deploy` 잡은 SSM 으로 서버에서 이걸 실행한다.

```
REPO=/home/ubuntu/deploy/backend
SHA=<github.sha>
# 클론이 없으면 클론하고, 있으면 해당 커밋을 받아온다
git -C $REPO reset --hard $SHA
bash $REPO/deploy/deploy.sh
```

`latest` 가 아니라 커밋 SHA 로 맞춘다. 실행되는 스크립트와 배포되는 이미지가
같은 커밋에서 나온다.

주의: SSM 의 `AWS-RunShellScript` 는 명령을 bash 가 아니라 `/bin/sh`(dash)로
돌린다. 워크플로의 `commands` 배열에는 bash 전용 문법(`pipefail` 등)을
쓸 수 없다. `deploy.sh` 는 `bash` 로 명시해 실행하므로 그 안에서는 상관없다.

## 무중단 — 블루-그린

컨테이너 두 개를 번갈아 쓴다.

| | 컨테이너 | 포트 |
|---|---|---|
| blue | `backend-blue` | `127.0.0.1:8081` |
| green | `backend-green` | `127.0.0.1:8082` |

평소에는 한 쪽만 떠 있고, 배포할 때만 30초 남짓 둘 다 떠 있다.

**트래픽 경로가 둘이라 각각 다르게 처리한다.**

- **외부** (nginx -> backend): `/etc/nginx/conf.d/backend-upstream.conf` 의
  `upstream backend_active` 를 새 포트로 바꾸고 `nginx -s reload`.
  reload 는 기존 연결을 끊지 않는다
- **내부** (frontend -> backend): compose 네트워크 **별칭**.
  blue 와 green 둘 다 `backend` 라는 별칭을 갖는다. frontend 는 예전처럼
  `http://backend:8080` 으로 부르면 되고, 옛 컨테이너가 내려가면 별칭은
  새 쪽만 가리킨다. **frontend 설정은 건드릴 필요가 없다**

순서:

```
1. idle 색으로 새 이미지 기동        (active 는 계속 서비스 중)
2. idle 헬스체크 UP 까지 대기
3. nginx upstream 을 idle 로 교체 + reload
4. 외부에서 https 로 다시 확인
5. 옛 active 내림
```

어느 단계에서 실패하든 **서비스 중인 색은 건드리지 않는다.** 새로 띄운 쪽만
정리하고 끝난다. 다운타임이 생기지 않는다. nginx 설정 검사나 전환 후 외부
헬스체크가 실패하면 upstream 을 원래대로 되돌린다.

### 메모리

t3.micro 는 램이 1GB 다. 교체 중에는 백엔드 컨테이너가 두 개 뜬다.

```
backend   214MB   (실측)
frontend    8MB
스왑      2GB     (이미 붙어 있음)
```

두 개여도 430MB 남짓이라 들어간다. 각 컨테이너에 `mem_limit: 700m` 을 걸어
한 쪽이 폭주해도 다른 쪽을 죽이지 못하게 했다.

### 전환 (한 번만)

서버에서 `setup-bluegreen.sh` 를 한 번 돌려야 한다. 그 전에는 `deploy.sh` 가
`backend-upstream.conf` 가 없다고 명확히 실패한다 (서비스에는 영향 없음).

```
cd /home/ubuntu/deploy/backend
sudo git fetch origin feat/zero-downtime-bluegreen
sudo git checkout FETCH_HEAD
sudo bash deploy/setup-bluegreen.sh
```

전환 스크립트도 다운타임 없이 간다. blue 를 먼저 띄우고, health 확인하고,
nginx 를 blue 로 돌린 다음에야 옛 `backend` 컨테이너를 내린다.

실패하면 nginx 설정(`/root/nginx-backup-*.tar.gz`)과 compose
(`docker-compose.yml.bak.*`)를 원래대로 되돌린다.
