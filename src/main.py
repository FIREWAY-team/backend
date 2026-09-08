import structlog
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from src.config import get_settings
from src.modules.cctv.interfaces.router import router as cctv_router
from src.modules.routing.interfaces.router import router as routing_router
from src.modules.scenarios.interfaces.router import router as scenarios_router
from src.modules.static_data.interfaces.router import router as static_data_router
from src.modules.vehicles.interfaces.router import router as vehicles_router
from src.shared.db.session import engine
from src.shared.exceptions.base import DomainError
from src.shared.logging import configure_logging
from src.shared.middleware.request_id import RequestIDMiddleware

configure_logging(get_settings().log_level)
logger = structlog.get_logger(__name__)
app = FastAPI(title="Golden Lane Backend", version="0.1.0", description="소방차 진입 판단 엔진")
app.add_middleware(RequestIDMiddleware)

def error_body(request: Request, code: str, message: str) -> dict:
    return {"error": {"code": code, "message": message, "request_id": request.state.request_id}}

@app.exception_handler(DomainError)
async def domain_error_handler(request: Request, exc: DomainError):
    return JSONResponse(content=error_body(request, exc.code, exc.message), status_code=exc.status_code)

@app.exception_handler(RequestValidationError)
async def validation_error_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(content=error_body(request, "REQUEST_VALIDATION_ERROR", str(exc.errors())), status_code=422)

@app.exception_handler(Exception)
async def unhandled_error_handler(request: Request, exc: Exception):
    logger.exception("unhandled_exception", error=str(exc))
    return JSONResponse(content=error_body(request, "INTERNAL_SERVER_ERROR", "Internal server error"), status_code=500)

@app.get("/health", tags=["system"])
async def health() -> dict:
    status = "ok"
    if get_settings().health_check_db:
        try:
            async with engine.connect() as connection:
                await connection.exec_driver_sql("SELECT 1")
        except Exception:
            status = "degraded"
    return {"status": status, "service": "golden-lane-backend"}

app.include_router(scenarios_router)
app.include_router(routing_router)
app.include_router(vehicles_router)
app.include_router(static_data_router)
app.include_router(cctv_router)
