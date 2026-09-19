"""Inspection endpoints.

POST /api/v1/inspections/upload  – accepts an image, runs the vision pipeline,
                                   evaluates compliance, and returns a report.
GET  /api/v1/inspections/{id}    – retrieves a previously stored inspection (stub).
"""
import shutil
import uuid
from pathlib import Path

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.config import settings
from app.engine.rules_registry import evaluate_inspection_compliance
from app.vision.pipeline import run_vision_pipeline

router = APIRouter(prefix="/inspections", tags=["Inspections"])


@router.post("/upload", summary="Upload an image and run compliance inspection")
async def upload_inspection_image(
    file: UploadFile = File(...),
    inspection_id: str = Form(default=None),
    package_type: str = Form("retail"),
    category: str = Form("General"),
):
    """Accepts a JPEG/PNG image upload, saves it to ``storage/raw/``, runs the
    full vision pipeline (ArUco → YOLO → OCR), evaluates the result against
    Legal Metrology statutory rules, and returns a structured compliance report.

    Args:
        file:          Uploaded image file (JPEG or PNG).
        inspection_id: Optional caller-supplied ID; auto-generated if omitted.
        package_type:  Commodity package type (e.g. "retail", "wholesale").
        category:      Product category for future rule branching.

    Returns:
        JSON with ``inspection_id``, ``saved_path``, and ``results`` (compliance report).
    """
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Uploaded file must be an image.")

    # Generate an inspection ID if not provided by the caller
    if not inspection_id:
        inspection_id = f"INS-{uuid.uuid4().hex[:8].upper()}"

    file_extension = Path(file.filename).suffix if file.filename else ".jpg"
    saved_filename = f"{inspection_id}_raw{file_extension}"
    target_path = settings.RAW_DIR / saved_filename

    # Persist the uploaded file
    with open(target_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # 1. Run Computer Vision Pipeline
    vision_results = run_vision_pipeline(str(target_path))

    # 2. Evaluate against Statutory Rules
    compliance_report = evaluate_inspection_compliance(vision_results, package_type)

    return {
        "inspection_id": inspection_id,
        "saved_path":    saved_filename,
        "results":       compliance_report,
    }


@router.get("/{inspection_id}", summary="Retrieve a stored inspection (stub)")
async def get_inspection(inspection_id: str):
    """Returns a stub inspection record.

    TODO: Query the real database once persistence is implemented.
    """
    return {
        "inspection_id": inspection_id,
        "status": "STUB — database persistence not yet implemented.",
    }
