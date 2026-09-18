# server/app/vision/pipeline.py
from typing import Dict, Any
from app.vision.aruco import calculate_scale
from app.vision.detector import detect_fields
from app.vision.ocr import extract_text_and_measure
# (Assume draw_and_save_annotations is already imported here)

def run_vision_pipeline(image_path: str) -> Dict[str, Any]:
    # 1. Get the deterministic scale
    scale_result = calculate_scale(image_path)
    mm_per_pixel = scale_result["mm_per_pixel"] if scale_result else 0.0612
    
    # 2. Get YOLO regions of interest (No math here anymore)
    detected_boxes = detect_fields(image_path)
    
    fields_data = []
    # 3. Extract text and measure the tight font boxes
    for item in detected_boxes:
        ocr_result = extract_text_and_measure(
            image_path, 
            tuple(item["bbox"]), 
            item["field"], 
            mm_per_pixel
        )
        
        # Merge YOLO's box data with PaddleOCR's text and height measurements
        fields_data.append({
            **item,
            **ocr_result
        })
        
    return {
        "scale": scale_result,
        "fields": fields_data
    }def run_vision_pipeline(image_path: str) -> Dict[str, Any]:
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