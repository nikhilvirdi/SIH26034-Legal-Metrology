from typing import Tuple, Dict, Any


def extract_text_from_crop(
    image_path: str,
    bbox: Tuple[int, int, int, int],
    field_name: str,
    mm_per_pixel: float,
) -> Dict[str, Any]:
    """Crops the image at `bbox`, saves the crop to `storage/crops/`, runs
    PaddleOCR to extract the printed text, and calculates the true physical
    height based on the tight ink polygon (not the YOLO bounding box).

    Args:
        image_path: Path to the full-resolution source image.
        bbox: Bounding box as (x1, y1, x2, y2) in pixels from YOLO.
        field_name: Human-readable label used for the saved crop filename and
                    mock lookup.
        mm_per_pixel: Scale conversion factor from ArUco calibration.

    Returns:
        Dictionary containing:
            - extracted_text: Recognized text string
            - text_height_px: Height of the tight text polygon in pixels
            - height_mm: Physical height of the text in millimeters
    """
    # STUB: Returns mock OCR text keyed by field name.
    # TODO: Replace with real PaddleOCR crop-and-read logic.
    _mock_values: dict[str, str] = {
        "mrp":           "Rs. 149.00 (incl. of all taxes)",
        "net_quantity":  "500 g",
        "mfg_date":      "08/2026",
        "consumer_care": "care@brand.in, 1800-111-222",
    }
    
    extracted_text = _mock_values.get(field_name, "DUMMY TEXT")
    
    # Mock text height in pixels (tight polygon, not YOLO box)
    # TODO: Replace with actual PaddleOCR polygon height calculation
    mock_text_height_px = 25
    
    # Calculate true physical height from tight ink polygon
    height_mm = round(mock_text_height_px * mm_per_pixel, 2)
    
    return {
        "extracted_text": extracted_text,
        "text_height_px": mock_text_height_px,
        "height_mm": height_mm,
    }
