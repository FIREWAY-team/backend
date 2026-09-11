# 배포

서버에만 있던 배포 파일들을 리포로 옮긴 것이다.
여기 있는 것이 정본이고, 서버는 이걸 받아서 쓴다.

## 왜 옮겼나

전에는 `deploy.sh` 와 운영 `docker-compose.yml` 이 EC2의 `~/deploy/` 에만 있었다.

- 버전 관리가 안 됐다. 누가 언제 뭘 바꿨는지 기록이 없다
- 리뷰를 못 했다. 배포 방식 변경이 PR에 안 잡혔다
- 서버가 날아가면 배포 방식도 같이 사라졌다

## 파일

| 파일 | 서버 위치 | 정본인가 |
|---|---|---|
| `deploy.sh` | `~/deploy/backend/deploy/deploy.sh` | **그렇다.** 배포가 이걸 실행한다 |
| `docker-compose.prod.yml` | `~/deploy/docker-compose.yml` | 아니다. 서버 파일의 거울 |

`deploy.sh` 는 정본이다. 워크플로의 SSM 명령이 매 배포마다 이 리포를
`github.sha` 로 맞춘 뒤 `deploy/deploy.sh` 를 실행한다.

`docker-compose.prod.yml` 은 아직 거울이다. `deploy.sh` 가 `COMPOSE_FILE`
기본값으로 `~/deploy/docker-compose.yml` 을 읽기 때문에, 배포는 여전히
서버 파일을 쓴다. **서버 compose 를 고치면 이 파일도 같이 고칠 것.**

서버 compose 에는 `backend` 와 `frontend` 가 같이 들어 있다.
프론트는 `BACKEND_API_URL: http://backend:8080` 으로 compose 네트워크의
서비스 이름을 통해 백엔드에 붙는다 (127.0.0.1 이 아니다).

서버에만 있고 리포에 없는 것:

- **`~/deploy/backend.env`** — 실제 DB 비밀번호가 들어 있다. 깃에 절대 넣지
  않는다 (`chmod 600`). 형식은 리포 루트의 `backend.env.example` 참고
- **`~/deploy/deploy-frontend.sh`** — 프론트 배포 스크립트. 이 리포 소관이
  아니라 옮기지 않았다. 프론트 리포로 옮기는 게 맞다
- `~/deploy/docker-compose.yml.bak` — 예전 백업

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

## 지금 무중단이 아니다

`deploy.sh` 의 `docker compose up -d` 는 기존 컨테이너를 내리고 새로 띄운다.
그 사이 수 초간 502가 난다.

1GB 램에서 블루그린을 하려면 컨테이너 두 개가 잠깐 같이 떠 있어야 하는데,
컨테이너당 실사용이 500~600MB라 그대로는 안 들어간다. 선택지는 셋이다.

- Nginx `proxy_next_upstream` + 재시도로 교체 순간을 덮는다 (체감 무중단)
- 힙을 256m 으로 낮춰 잠깐 두 개를 띄운다 (GC 압박이 커진다)
- t3.small(2GB)로 올린다 (가장 확실하고, 돈이 든다)

별도 작업으로 다룬다.
