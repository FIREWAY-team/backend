from fastapi import APIRouter
router = APIRouter(tags=["scenarios"])
@router.get("/scenarios")
async def scenarios():
    return {"scenarios": [{"id": "demo-01", "name": "성남 구도심 골목 시연", "description": "CCTV 판독과 정적 진입불가 데이터를 활용한 모의 상황", "status": "ready"}]}

