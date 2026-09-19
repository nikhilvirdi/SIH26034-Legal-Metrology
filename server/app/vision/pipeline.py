from pathlib import Path
from typing import Any, Dict, List

import cv2

from app.config import settings
from app.vision.aruco import calculate_scale
from app.vision.detector import detect_fields
from app.vision.ocr import extract_text_from_crop


def draw_and_save_annotations(
    raw_image_path: str, fields: List[Dict[str, Any]], filename: str
) -> str:
    """Draw bounding boxes and labels on the image and save the annotated result.

    Args:
        raw_image_path: Path to the original image file.
        fields: List of detected field dictionaries containing bbox, field name, and height_mm.
        filename: Output filename for the annotated image.

    Returns:
        str: Path to the saved annotated image, or empty string if image loading fails.
    """
    # Load the image
    image = cv2.imread(raw_image_path)
    
    if image is None:
        print(f"Warning: Could not load image at {raw_image_path}")
        return ""
    
    # Define colors for different field types (BGR format for OpenCV)
    color_map = {
        "mrp": (0, 255, 0),          # Green
        "brand_name": (255, 0, 0),    # Blue
        "manufacturer": (0, 165, 255), # Orange
        "net_quantity": (255, 0, 255), # Magenta
        "default": (0, 255, 255)      # Yellow
    }
    
    # Draw bounding boxes and labels
    for field_data in fields:
        bbox = field_data.get("bbox", [])
        if len(bbox) != 4:
            continue
            
        x1, y1, x2, y2 = map(int, bbox)
        field_name = field_data.get("field", "unknown")
        height_mm = field_data.get("height_mm", 0.0)
        
        # Get color for this field type
        color = color_map.get(field_name, color_map["default"])
        
        # Draw rectangle
        cv2.rectangle(image, (x1, y1), (x2, y2), color, 3)
        
        # Prepare label text
        label = f"{field_name}: {height_mm:.2f}mm"
        
        # Calculate text size for background
        font = cv2.FONT_HERSHEY_SIMPLEX
        font_scale = 0.7
        thickness = 2
        (text_width, text_height), baseline = cv2.getTextSize(
            label, font, font_scale, thickness
        )
        
        # Draw background rectangle for text
        text_y = max(y1 - 10, text_height + 10)
        cv2.rectangle(
            image,
            (x1, text_y - text_height - baseline),
            (x1 + text_width, text_y + baseline),
            color,
            -1  # Filled rectangle
        )
        
        # Draw text
        cv2.putText(
            image,
            label,
            (x1, text_y - baseline),
            font,
            font_scale,
            (255, 255, 255),  # White text
            thickness,
            cv2.LINE_AA
        )
    
    # Save annotated image
    output_path = settings.ANNOTATED_DIR / filename
    cv2.imwrite(str(output_path), image)
    
    return str(output_path)


def run_vision_pipeline(image_path: str) -> Dict[str, Any]:
    """Orchestrates the full vision pipeline for a single image:

    1. ArUco scale extraction  → mm_per_pixel conversion factor
    2. YOLOv8 label detection  → bounding boxes (no height calculation yet)
    3. Crop generation & OCR   → extracted text + tight polygon height measurement
    4. Annotation visualization → draw bounding boxes on image

    Args:
        image_path: Path to the uploaded/stored high-resolution image.

    Returns:
        dict with keys:
            scale            – raw output from :func:`calculate_scale`
            fields           – list of dicts (bbox, confidence, extracted_text, height_mm)
            annotated_image  – path to the saved annotated image with bounding boxes
    """
    # Step 1: Get scale from ArUco marker
    scale_result = calculate_scale(image_path)
    mm_per_pixel: float = scale_result["mm_per_pixel"] if scale_result else 0.0612

    # Step 2: Get YOLO bounding boxes (no height calculation)
    detected_boxes = detect_fields(image_path)

    # Step 3: Run OCR and calculate physical height from tight text polygon
    fields_data = []
    for item in detected_boxes:
        # OCR now returns a dict with extracted_text, text_height_px, and height_mm
        ocr_result = extract_text_from_crop(
            image_path, 
            tuple(item["bbox"]), 
            item["field"],
            mm_per_pixel
        )
        
        # Merge YOLO data with OCR data
        fields_data.append({
            **item,                    # field, bbox, confidence from YOLO
            **ocr_result               # extracted_text, text_height_px, height_mm from OCR
        })

    # Step 4: Generate annotated image with bounding boxes
    filename = Path(image_path).name
    annotated_filename = f"annotated_{filename}"
    annotated_path = draw_and_save_annotations(image_path, fields_data, annotated_filename)

    return {
        "scale": scale_result,
        "fields": fields_data,
        "annotated_image": annotated_path,
    }
