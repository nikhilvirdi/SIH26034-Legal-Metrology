# ArUco Marker Generation Guide

## Overview
This guide explains how to generate and print the exact 40mm × 40mm ArUco marker required for the Legal Metrology Inspector app's scale calibration system.

## Marker Specifications

| Property | Value |
|----------|-------|
| Dictionary | `DICT_6X6_100` |
| Marker ID | 0 (recommended, any ID from dictionary works) |
| Physical Size | 40mm × 40mm (outer black border) |
| Total Perimeter | 160mm |
| Print Resolution | Minimum 300 DPI |
| Paper Type | White, matte, non-glossy |

## Generation Methods

### Method 1: Using Python + OpenCV (Recommended)

```python
import cv2
import numpy as np

# Generate ArUco marker
aruco_dict = cv2.aruco.getPredefinedDictionary(cv2.aruco.DICT_6X6_100)
marker_id = 0
marker_size = 400  # pixels (will be scaled to 40mm when printed)

# Generate marker image
marker_image = cv2.aruco.generateImageMarker(aruco_dict, marker_id, marker_size)

# Add white border for safety (optional but recommended)
border_size = 40
bordered_image = cv2.copyMakeBorder(
    marker_image, 
    border_size, border_size, border_size, border_size,
    cv2.BORDER_CONSTANT, 
    value=255
)

# Save marker
cv2.imwrite('aruco_marker_40mm.png', bordered_image)
print("ArUco marker generated: aruco_marker_40mm.png")
```

### Method 2: Using Online Generator

1. Visit: https://chev.me/arucogen/ or similar ArUco generator
2. Select Dictionary: `6x6 (50, 100, 250, 1000)`
3. Enter Marker ID: `0`
4. Set Size: `40mm` (or input pixels at 300 DPI: ~472px)
5. Download PNG/PDF

### Method 3: Using C++ OpenCV

```cpp
#include <opencv2/opencv.hpp>
#include <opencv2/aruco.hpp>

int main() {
    cv::aruco::Dictionary dictionary = 
        cv::aruco::getPredefinedDictionary(cv::aruco::DICT_6X6_100);
    
    cv::Mat markerImage;
    int markerId = 0;
    int markerSizePx = 400;
    
    cv::aruco::generateImageMarker(dictionary, markerId, markerSizePx, markerImage);
    
    // Add white border
    cv::Mat bordered;
    cv::copyMakeBorder(markerImage, bordered, 40, 40, 40, 40, 
                       cv::BORDER_CONSTANT, cv::Scalar(255));
    
    cv::imwrite("aruco_marker_40mm.png", bordered);
    return 0;
}
```

## Printing Instructions

### Preparation
1. Use a high-quality printer (laser printer recommended, inkjet acceptable)
2. Load white, matte paper (80-120 GSM)
3. Ensure printer is calibrated and has sufficient toner/ink

### Print Settings
- **Resolution**: 300 DPI or higher
- **Color**: Black & White (grayscale acceptable)
- **Scaling**: **100% (NO SCALING!)** - This is critical
- **Paper Size**: A4 or Letter
- **Margins**: Default (marker will be centered)

### Size Verification
**CRITICAL**: After printing, measure the outer black border with a ruler:
- **Must be exactly 40mm × 40mm** (±0.5mm tolerance)
- If incorrect, adjust print scale and reprint
- Use a digital caliper for precise measurement if available

### Calculation for Print Size
If your marker image is 480px × 480px (including border):
- At 300 DPI: 480px ÷ 300 DPI × 25.4mm/inch = **40.64mm** ✓
- At 72 DPI (screen): 480px ÷ 72 DPI × 25.4mm/inch = **169mm** ✗

## Post-Printing Steps

### 1. Measure & Verify
```
┌─────────────────────────┐
│ White Border (optional) │
│  ┌───────────────────┐  │
│  │ ■ ■ ■ ■ ■ ■ │  │
│  │ ■ □ □ □ □ ■ │  │
│  │ ■ □ ■ ■ □ ■ │ 40mm
│  │ ■ □ ■ ■ □ ■ │  │
│  │ ■ □ □ □ □ ■ │  │
│  │ ■ ■ ■ ■ ■ ■ │  │
│  └───────────────────┘  │
│      40mm               │
└─────────────────────────┘
```
Measure the outer black border precisely.

### 2. Laminate (Recommended)
- Use a cold lamination pouch or self-adhesive laminate
- Avoid hot lamination (can cause warping)
- Trim excess laminate, keeping white border

### 3. Mount on Rigid Surface
- Attach to plastic card (credit card size) or cardboard
- Ensure surface is perfectly flat (no bends or warps)
- Use double-sided tape or spray adhesive

### 4. Test Detection
- Open the app and point camera at marker
- Should detect and lock instantly (< 0.5 seconds)
- If not detecting, check:
  - Marker is flat (no folds or curves)
  - No glare or reflections
  - Adequate lighting (not too dark, not too bright)
  - Camera lens is clean

## Multiple Markers

Generate a full sheet of markers for redundancy:

```python
import cv2
import numpy as np

aruco_dict = cv2.aruco.getPredefinedDictionary(cv2.aruco.DICT_6X6_100)

# Create A4 canvas at 300 DPI (2480 × 3508 pixels)
canvas = np.ones((3508, 2480), dtype=np.uint8) * 255

# Marker size: 40mm at 300 DPI = 472 pixels
marker_size = 472
spacing = 100  # pixels between markers

positions = [
    (200, 200),
    (200, 200 + marker_size + spacing),
    (200, 200 + 2*(marker_size + spacing)),
    (200 + marker_size + spacing, 200),
    (200 + marker_size + spacing, 200 + marker_size + spacing),
]

for i, (x, y) in enumerate(positions):
    marker = cv2.aruco.generateImageMarker(aruco_dict, i, marker_size)
    canvas[y:y+marker_size, x:x+marker_size] = marker
    
    # Add ID label
    cv2.putText(canvas, f"ID: {i}", (x, y-10), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, 0, 2)

cv2.imwrite('aruco_markers_sheet.png', canvas)
```

## Troubleshooting

### Marker Not Detected
- ✅ Check marker size (must be exactly 40mm)
- ✅ Ensure marker is flat and not warped
- ✅ Verify adequate lighting (avoid shadows)
- ✅ Clean camera lens
- ✅ Check for print quality (sharp edges, no smudging)

### Inconsistent Scale Readings
- ✅ Verify marker dimensions with caliper
- ✅ Check for glossy surface (causes reflections)
- ✅ Ensure marker is perpendicular to camera
- ✅ Re-laminate if surface is damaged

### Low Detection Speed
- ✅ Use higher contrast printing (pure black on white)
- ✅ Increase lighting
- ✅ Clean marker surface
- ✅ Reduce camera exposure if overexposed

## Distribution to Field Officers

### Option 1: Pre-Printed Cards
- Print and laminate markers in bulk
- Distribute as standard equipment
- Include in officer's inspection kit

### Option 2: Digital Distribution + Local Printing
- Distribute PNG/PDF files to field offices
- Each office prints markers locally
- Standardize printing instructions

### Option 3: Mobile App Generation (Future)
- Add marker generator to app settings
- Officer can generate and print on-demand
- Display marker on second device screen (not recommended for production use)

## Quality Control

Before deploying markers to field officers:
1. Measure 10 sample markers from print batch
2. Average dimension should be 40.0mm ± 0.2mm
3. Test detection rate (should be > 95% in normal lighting)
4. Verify scale calibration against known reference objects
5. Document batch number and distribution date

## References

- OpenCV ArUco Documentation: https://docs.opencv.org/4.x/d5/dae/tutorial_aruco_detection.html
- ArUco Marker Detector: https://docs.opencv.org/4.x/d9/d6a/group__aruco.html
- Original ArUco Paper: Garrido-Jurado et al., "Automatic generation and detection of highly reliable fiducial markers under occlusion" (2014)

---

**Last Updated**: September 17, 2026
**Maintained By**: Legal Metrology Inspector App Development Team
