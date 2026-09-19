"""Standard-pack-size validator.

Legal Metrology (Packaged Commodities) Rules 2011 specify permitted quantities
for various commodity categories.  This module provides a stub lookup.
"""
from typing import Dict, List, Optional

# Permitted weights/volumes in grams or millilitres per commodity category.
# Values are illustrative; expand once the full Schedule is digitised.
PERMITTED_SIZES: Dict[str, List[float]] = {
    "food_solid":   [25, 50, 100, 200, 250, 500, 1000, 2000, 5000],
    "food_liquid":  [50, 100, 200, 500, 1000, 2000, 5000],
    "cosmetics":    [25, 50, 100, 150, 200, 500],
    "detergent":    [100, 200, 500, 1000, 2000],
}


def is_permitted_size(
    quantity_g_or_ml: float,
    category: str = "food_solid",
) -> Dict[str, object]:
    """Check whether a declared net quantity is in the permitted list.

    Args:
        quantity_g_or_ml: Declared net quantity (grams or millilitres).
        category: Commodity category key from PERMITTED_SIZES.

    Returns:
        dict with:
            passed      – bool
            quantity    – the queried quantity
            category    – the queried category
            permitted   – list of allowed values (or None if category unknown)
            message     – human-readable verdict
    """
    permitted: Optional[List[float]] = PERMITTED_SIZES.get(category)
    if permitted is None:
        return {
            "passed": True,
            "quantity": quantity_g_or_ml,
            "category": category,
            "permitted": None,
            "message": f"Unknown category '{category}' — defaulting to PASS.",
        }
    passed = quantity_g_or_ml in permitted
    return {
        "passed": passed,
        "quantity": quantity_g_or_ml,
        "category": category,
        "permitted": permitted,
        "message": (
            f"{quantity_g_or_ml} g/ml is a permitted size for '{category}'."
            if passed
            else f"{quantity_g_or_ml} g/ml is NOT a permitted size for '{category}'."
        ),
    }
