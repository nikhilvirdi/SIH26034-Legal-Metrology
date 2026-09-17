from typing import Optional

import numpy as np  # noqa: F401  (will be used in real implementation)


def calculate_scale(image_path: str, known_size_mm: float = 40.0) -> Optional[dict]:
    """Detects a 40 mm DICT_6X6_100 ArUco marker in the high-res image.

    Args:
        image_path: Absolute or relative path to the source image.
        known_size_mm: Physical side length of the printed ArUco marker in millimetres.

    Returns:
        dict with keys:
            mm_per_pixel  – conversion factor derived from marker perimeter
            corners       – four corner points [[x,y], ...]
            marker_id     – integer ID of the detected marker
            pixel_perimeter – total pixel perimeter of the detected marker
        or None if no marker is detected.
    """
    # STUB: Returns a deterministic mock scale for initial wiring.
    # TODO: Replace with real cv2.aruco detection once weights & env are ready.
    return {
        "mm_per_pixel": 0.0612,
        "corners": [[100, 100], [200, 100], [200, 200], [100, 200]],
        "marker_id": 0,
        "pixel_perimeter": 653.0,
    }
