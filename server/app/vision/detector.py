from typing import List, Dict, Any
from ultralytics import YOLO
from app.config import settings

# Load the model globally at startup so it remains in memory across API requests.
# This prevents the massive overhead of reloading the .pt file on every scan.
try:
    model = YOLO(str(settings.WEIGHTS_DIR / "yolov8_metrology.pt"))
except FileNotFoundError:
    print("Warning: yolov8_metrology.pt not found. Ensure the weights are placed in server/weights/")
    model = None

def detect_fields(image_path: str, mm_per_pixel: float) -> List[Dict[str, Any]]:
    """
    Runs YOLOv8 model inference over the high-res image to locate mandatory packaging fields.
    Computes real-world physical height (mm) for each bounding box:
        height_mm = bbox_height_px * mm_per_pixel
    """
    if not model:
        raise RuntimeError("YOLO model weights are missing from the weights directory.")

    # Run inference on the provided image
    results = model(image_path)
    detected_fields = []

    # results[0] contains the predictions for the single image processed
    for box in results[0].boxes:
        # Extract coordinates [x1, y1, x2, y2], confidence, and class ID
        x1, y1, x2, y2 = box.xyxy[0].tolist()
        confidence = float(box.conf[0].item())
        cls_id = int(box.cls[0].item())
        
        # Map class ID to the string name (e.g., "mrp", "net_quantity")
        field_name = model.names[cls_id]

        # Calculate dimensions
        height_px = y2 - y1
        height_mm = round(height_px * mm_per_pixel, 2)

        detected_fields.append({
            "field": field_name,
            "bbox": [int(x1), int(y1), int(x2), int(y2)],
            "confidence": round(confidence, 4),
            "height_px": int(height_px),
            "height_mm": height_mm
        })

    return detected_fields