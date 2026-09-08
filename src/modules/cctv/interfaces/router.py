from fastapi import APIRouter, Path
router = APIRouter(tags=["cctv"])
@router.get("/cctv/{edge_id}")
async def cctv(edge_id: str = Path(..., examples=["edge-001"])):
    return {"edge_id": edge_id, "still_url": "https://example.com/mock/cctv-edge-001.jpg", "classification": {"blocked": False, "confidence": 0.93, "reason": "차량 통행 가능으로 판독된 모의 결과"}, "captured_at": "2026-09-08T00:00:00Z"}

