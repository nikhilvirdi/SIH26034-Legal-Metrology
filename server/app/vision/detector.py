from typing import Any, Dict, List


def detect_fields(image_path: str, mm_per_pixel: float) -> List[Dict[str, Any]]:
    """Runs YOLOv8 model inference over the high-res image to locate mandatory
    packaging fields and computes their real-world physical dimensions.

    Args:
        image_path: Path to the source image.
        mm_per_pixel: Conversion factor from ArUco calibration.

    Returns:
        List of dicts, each containing:
            field       – label name (e.g. "mrp", "net_quantity")
            bbox        – [x1, y1, x2, y2] in pixels
            confidence  – YOLO detection confidence score
            height_px   – bounding-box height in pixels
            height_mm   – physical height (height_px × mm_per_pixel)
    """
    # STUB: Returns sample packaging regions with computed physical heights.
    # TODO: Replace with real ultralytics YOLO inference once weights are present.
    _fields = [
        {"field": "mrp",            "bbox": [320, 450, 480, 510], "confidence": 0.94, "height_px": 60},
        {"field": "net_quantity",   "bbox": [320, 520, 500, 580], "confidence": 0.91, "height_px": 60},
        {"field": "mfg_date",       "bbox": [150, 700, 350, 750], "confidence": 0.88, "height_px": 50},
    ]
    return [
        {**f, "height_mm": round(f["height_px"] * mm_per_pixel, 2)}
        for f in _fields
    ]
