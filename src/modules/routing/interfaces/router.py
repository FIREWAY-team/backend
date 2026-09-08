from typing import Any
from fastapi import APIRouter
from pydantic import BaseModel, Field

router = APIRouter(tags=["routing"])
class RouteRequest(BaseModel):
    origin: dict[str, float] = Field(examples=[{"lat": 37.44, "lon": 127.14}])
    destination: dict[str, float] = Field(examples=[{"lat": 37.45, "lon": 127.15}])
    vehicle_id: str = "fire_engine_01"
class RouteResponse(BaseModel):
    route_id: str
    vehicle_id: str
    accessibility_probability: float
    llm_reason: str
    geometry: dict[str, Any]

@router.post("/route", response_model=RouteResponse)
async def calculate_route(payload: RouteRequest):
    return {"route_id": "mock-route-001", "vehicle_id": payload.vehicle_id, "accessibility_probability": 0.87, "llm_reason": "정적 진입불가 구간을 회피하는 모의 경로입니다.", "geometry": {"type": "LineString", "coordinates": [[127.14, 37.44], [127.15, 37.45]]}}

