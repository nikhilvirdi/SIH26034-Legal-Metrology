from typing import Any, Dict

from app.vision.aruco import calculate_scale
from app.vision.detector import detect_fields
from app.vision.ocr import extract_text_from_crop


def run_vision_pipeline(image_path: str) -> Dict[str, Any]:
    """Orchestrates the full vision pipeline for a single image:

    1. ArUco scale extraction  → mm_per_pixel conversion factor
    2. YOLOv8 label detection  → bounding boxes + physical dimensions
    3. Crop generation & OCR   → extracted text per field

    Args:
        image_path: Path to the uploaded/stored high-resolution image.

    Returns:
        dict with keys:
            scale  – raw output from :func:`calculate_scale`
            fields – list of dicts (bbox, confidence, height_mm, extracted_text)
    """
    scale_result = calculate_scale(image_path)
    mm_per_pixel: float = scale_result["mm_per_pixel"] if scale_result else 0.0612

    detected_boxes = detect_fields(image_path, mm_per_pixel)

    fields_data = []
    for item in detected_boxes:
        text = extract_text_from_crop(image_path, tuple(item["bbox"]), item["field"])
        fields_data.append({**item, "extracted_text": text})

    return {
        "scale": scale_result,
        "fields": fields_data,
    }
