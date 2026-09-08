from fastapi import APIRouter, Query
router = APIRouter(tags=["vehicles"])
@router.get("/vehicles")
async def vehicles():
    return {"vehicles": [{"id": "fire_engine_01", "name": "성남 소방펌프차", "width_m": 2.5, "height_m": 3.1, "length_m": 8.0, "turning_radius_m": 9.0}]}
@router.get("/coverage")
async def coverage(vehicle_id: str = Query(..., examples=["fire_engine_01"])):
    return {"vehicle_id": vehicle_id, "coverage": {"type": "FeatureCollection", "features": []}, "reachable_edge_count": 0, "source": "mock"}

