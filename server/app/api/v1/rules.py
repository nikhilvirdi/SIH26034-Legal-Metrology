"""Compliance-rules endpoints.

GET /rules/              – list all available rule keys
GET /rules/{field_name} – describe the rule for a specific field
"""
from typing import Any, Dict, List

from fastapi import APIRouter, HTTPException

from app.engine.font_checker import MIN_HEIGHT_MM
from app.engine.pack_sizes import PERMITTED_SIZES

router = APIRouter(prefix="/rules", tags=["rules"])


@router.get("/", response_model=List[str], summary="List all rule keys")
async def list_rules() -> List[str]:
    """Returns the union of field names that have font-height or pack-size rules."""
    keys = sorted(set(MIN_HEIGHT_MM.keys()) | set(PERMITTED_SIZES.keys()))
    return keys


@router.get("/{field_name}", response_model=Dict[str, Any], summary="Describe a rule")
async def get_rule(field_name: str) -> Dict[str, Any]:
    """Returns the compliance constraints for a given field."""
    font_rule = MIN_HEIGHT_MM.get(field_name)
    size_rule = PERMITTED_SIZES.get(field_name)

    if font_rule is None and size_rule is None:
        raise HTTPException(status_code=404, detail=f"No rules found for field '{field_name}'.")

    return {
        "field": field_name,
        "min_font_height_mm": font_rule,
        "permitted_sizes": size_rule,
    }
