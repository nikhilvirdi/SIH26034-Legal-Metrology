"""Font / numeral-height compliance checker.

Implements Rule 7 of the Legal Metrology (Packaged Commodities) Rules 2011,
which mandates minimum printed numeral heights based on the label surface area.

Reference: G.S.R. 629(E) — Table under Rule 7.
"""
from typing import Dict

# Minimum character heights in mm as per PC Rules 2011 (Rule 7 tier table).
# Keys match the field labels used by the YOLO detector.
MIN_HEIGHT_MM: Dict[str, float] = {
    "mrp":           1.0,
    "net_quantity":  2.0,
    "mfg_date":      1.0,
    "exp_date":      1.0,
    "consumer_care": 1.0,
    "manufacturer":  1.0,
}


def check_font_height(field_name: str, height_mm: float) -> Dict[str, object]:
    """Verify that the detected character height meets the legal minimum.

    Args:
        field_name: Label identifier (must match keys in MIN_HEIGHT_MM).
        height_mm:  Measured character height derived from ArUco scale.

    Returns:
        dict with:
            passed      – bool
            required_mm – minimum required height (or None if no rule defined)
            actual_mm   – measured height
            message     – human-readable verdict
    """
    required = MIN_HEIGHT_MM.get(field_name)
    if required is None:
        return {
            "passed": True,
            "required_mm": None,
            "actual_mm": height_mm,
            "message": f"No height requirement defined for '{field_name}'.",
        }
    passed = height_mm >= required
    return {
        "passed": passed,
        "required_mm": required,
        "actual_mm": round(height_mm, 3),
        "message": (
            f"'{field_name}' height {height_mm:.2f} mm meets the {required} mm requirement."
            if passed
            else f"'{field_name}' height {height_mm:.2f} mm is BELOW the {required} mm requirement."
        ),
    }


def check_numeral_height(
    field_name: str,
    measured_height_mm: float,
    label_area_cm2: float = 250.0,
) -> dict:
    """Evaluates the Rule 7 font-size tier table for packaged commodities.

    Tier logic (G.S.R. 629(E) Rule 7 Table):
        label_area_cm2 <=  100  → required minimum = 1.5 mm
        label_area_cm2  > 100  → required minimum = 2.5 mm

    Args:
        field_name:          Label identifier (e.g. "mrp", "net_quantity").
        measured_height_mm:  Character height measured via ArUco px→mm scale.
        label_area_cm2:      Total printable label area in cm².  Defaults to
                             250 cm², which falls in the 100–500 cm² tier.

    Returns:
        dict with:
            field              – field name queried
            measured_height_mm – as supplied
            required_min_mm    – tier-based minimum
            is_compliant       – bool
            citation           – statutory reference string
    """
    required_min_mm = 2.5 if label_area_cm2 > 100 else 1.5
    is_compliant = measured_height_mm >= required_min_mm

    return {
        "field": field_name,
        "measured_height_mm": measured_height_mm,
        "required_min_mm": required_min_mm,
        "is_compliant": is_compliant,
        "citation": "Rule 7, Table (100–500 cm²) - G.S.R. 629(E)",
    }
