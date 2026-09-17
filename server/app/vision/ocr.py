from typing import Tuple


def extract_text_from_crop(
    image_path: str,
    bbox: Tuple[int, int, int, int],
    field_name: str,
) -> str:
    """Crops the image at `bbox`, saves the crop to `storage/crops/`, and runs
    PaddleOCR to extract the printed text.

    Args:
        image_path: Path to the full-resolution source image.
        bbox: Bounding box as (x1, y1, x2, y2) in pixels.
        field_name: Human-readable label used for the saved crop filename and
                    mock lookup.

    Returns:
        Recognised text string from the cropped region.
    """
    # STUB: Returns mock OCR text keyed by field name.
    # TODO: Replace with real PaddleOCR crop-and-read logic.
    _mock_values: dict[str, str] = {
        "mrp":           "Rs. 149.00 (incl. of all taxes)",
        "net_quantity":  "500 g",
        "mfg_date":      "08/2026",
        "consumer_care": "care@brand.in, 1800-111-222",
    }
    return _mock_values.get(field_name, "DUMMY TEXT")
