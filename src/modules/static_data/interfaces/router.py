from fastapi import APIRouter, Query
router = APIRouter(tags=["static-data"])
@router.get("/no_go")
async def no_go(bbox: str | None = Query(None), layer: str | None = Query(None)):
    return {"type": "FeatureCollection", "features": [], "meta": {"bbox": bbox, "layer": layer, "source": "mock"}}

