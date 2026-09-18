from typing import List, Dict, Any
from ultralytics import YOLO
from app.config import settings

try:
    model = YOLO(str(settings.WEIGHTS_DIR / "yolov8_metrology.pt"))
except FileNotFoundError:
    model = None

def detect_fields(image_path: str) -> List[Dict[str, Any]]:
    """
    Runs YOLOv8 model inference over the high-res image to locate mandatory packaging fields.
    Returns only the field name, coordinates, and confidence.
    """
    if not model:
        raise RuntimeError("YOLO model weights are missing.")

    results = model(image_path)
    detected_fields = []

    # GUARD: If YOLO finds absolutely nothing (e.g., plain background), return empty list
    if not results or len(results[0].boxes) == 0:
        return detected_fields

    for box in results[0].boxes:
        # Some versions of ultralytics require checking if box is valid
        if box is None:
            continue
            
        x1, y1, x2, y2 = box.xyxy[0].tolist()
        confidence = float(box.conf[0].item())
        cls_id = int(box.cls[0].item())
        field_name = model.names[cls_id]

        detected_fields.append({
            "field": field_name,
            "bbox": [int(x1), int(y1), int(x2), int(y2)],
            "confidence": round(confidence, 4)
        })

    return detected_fields