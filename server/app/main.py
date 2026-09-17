"""FastAPI application entry point.

Registers all v1 API routers and exposes health / readiness endpoints.
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.inspections import router as inspections_router
from app.config import settings

app = FastAPI(
    title=settings.APP_NAME,
    debug=settings.DEBUG,
    version="1.0.0",
    description=(
        "Legal Metrology Inspection Engine — unified backend for ArUco-calibrated "
        "YOLOv8 + PaddleOCR label analysis and PC Rules 2011 compliance evaluation."
    ),
    docs_url="/docs",
    redoc_url="/redoc",
)

# ---------------------------------------------------------------------------
# CORS — wide open during development; restrict origins in production
# ---------------------------------------------------------------------------
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------------------------------------------------------------------
# Routers
# ---------------------------------------------------------------------------
app.include_router(inspections_router, prefix="/api/v1")


# ---------------------------------------------------------------------------
# Health endpoints
# ---------------------------------------------------------------------------
@app.get("/health", tags=["meta"], summary="Liveness probe")
def health_check() -> dict:
    return {"status": "ok", "app": settings.APP_NAME}


@app.get("/ready", tags=["meta"], summary="Readiness probe")
def ready_check() -> dict:
    """Confirms that all storage directories are accessible."""
    return {
        "status":        "ready",
        "raw_dir":       str(settings.RAW_DIR),
        "crops_dir":     str(settings.CROPS_DIR),
        "annotated_dir": str(settings.ANNOTATED_DIR),
    }
