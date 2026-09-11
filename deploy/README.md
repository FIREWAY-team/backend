# 배포

서버에만 있던 배포 파일들을 리포로 옮긴 것이다.
여기 있는 것이 정본이고, 서버는 이걸 받아서 쓴다.

## 왜 옮겼나

전에는 `deploy.sh` 와 운영 `docker-compose.yml` 이 EC2의 `~/deploy/` 에만 있었다.

- 버전 관리가 안 됐다. 누가 언제 뭘 바꿨는지 기록이 없다
- 리뷰를 못 했다. 배포 방식 변경이 PR에 안 잡혔다
- 서버가 날아가면 배포 방식도 같이 사라졌다

## 파일

| 파일 | 서버 위치 |
|---|---|
| `deploy.sh` | `~/deploy/backend/deploy/deploy.sh` (리포 클론 안) |
| `docker-compose.prod.yml` | `~/deploy/docker-compose.yml` |

서버에만 있고 리포에 없는 것: **`~/deploy/backend.env`**.
실제 DB 비밀번호가 들어 있어 깃에 절대 넣지 않는다 (`chmod 600`).
형식은 리포 루트의 `backend.env.example` 참고.

## 서버 전환 절차 (한 번만)

서버에서:

```
cd ~/deploy
git clone https://github.com/FIREWAY-team/backend.git
```

기존 `~/deploy/deploy.sh` 와 이 리포의 `deploy/deploy.sh` 를 **먼저 비교한다.**

```
diff ~/deploy/deploy.sh ~/deploy/backend/deploy/deploy.sh
```

리포 쪽 스크립트는 서버 실물을 못 본 상태에서 README의 수동 배포 절차를 근거로
다시 쓴 것이다. 서버 쪽에만 있는 단계(마이그레이션, 알림, 백업 등)가 있으면
리포 쪽에 반영한 뒤에 전환할 것.

compose 파일도 같은 방식으로 비교한다.

```
diff ~/deploy/docker-compose.yml ~/deploy/backend/deploy/docker-compose.prod.yml
```

둘 다 확인됐으면 워크플로의 SSM 명령을 바꾼다.

```
# 지금
commands=["su - ubuntu -c /home/ubuntu/deploy/deploy.sh"]

# 전환 후
commands=["su - ubuntu -c 'git -C /home/ubuntu/deploy/backend pull --ff-only && bash /home/ubuntu/deploy/backend/deploy/deploy.sh'"]
```

## 지금 무중단이 아니다

`deploy.sh` 의 `docker compose up -d` 는 기존 컨테이너를 내리고 새로 띄운다.
그 사이 수 초간 502가 난다.

1GB 램에서 블루그린을 하려면 컨테이너 두 개가 잠깐 같이 떠 있어야 하는데,
컨테이너당 실사용이 500~600MB라 그대로는 안 들어간다. 선택지는 셋이다.

- Nginx `proxy_next_upstream` + 재시도로 교체 순간을 덮는다 (체감 무중단)
- 힙을 256m 으로 낮춰 잠깐 두 개를 띄운다 (GC 압박이 커진다)
- t3.small(2GB)로 올린다 (가장 확실하고, 돈이 든다)

별도 작업으로 다룬다.
