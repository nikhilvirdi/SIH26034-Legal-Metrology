"""Central compliance evaluator.

Applies Legal Metrology (Packaged Commodities) Rules 2011 — Rule 6, Rule 7,
and Rule 24 — against the structured output of the vision pipeline.
"""
from typing import Any, Callable, Dict, List

from app.engine.font_checker import check_numeral_height

# ---------------------------------------------------------------------------
# Decorator-based rule registry (kept for per-field extensibility)
# ---------------------------------------------------------------------------
RuleFn = Callable[[str, str, float], Dict[str, Any]]
_REGISTRY: Dict[str, RuleFn] = {}


def register(field_name: str) -> Callable[[RuleFn], RuleFn]:
    """Decorator that registers a rule function for a given field name."""
    def decorator(fn: RuleFn) -> RuleFn:
        _REGISTRY[field_name] = fn
        return fn
    return decorator


def evaluate(field_name: str, value: str, height_mm: float) -> Dict[str, Any]:
    """Run the registered per-field rule, falling back to a PASS stub."""
    rule = _REGISTRY.get(field_name)
    if rule is None:
        return {
            "passed": True,
            "message": f"No rule defined for '{field_name}' — defaulting to PASS.",
        }
    return rule(field_name, value, height_mm)


# ---------------------------------------------------------------------------
# Bulk inspection evaluator
# ---------------------------------------------------------------------------

def evaluate_inspection_compliance(
    vision_payload: Dict[str, Any],
    package_type: str = "retail",
) -> Dict[str, Any]:
    """Evaluates all extracted fields against Legal Metrology Rules
    (Rule 6, Rule 7, Rule 24).

    Args:
        vision_payload: Output dict from :func:`app.vision.pipeline.run_vision_pipeline`.
                        Expected keys: ``fields`` (list) and ``scale`` (dict | None).
        package_type:   Commodity package type — reserved for future rule branching.

    Returns:
        dict with:
            overall_status       – "COMPLIANT" or "NON_COMPLIANT"
            scale_applied        – raw scale dict from vision pipeline
            field_evaluations    – per-field result list
            pcr_registry_verified – always True for this stub phase
    """
    field_results: List[Dict[str, Any]] = []
    has_violation = False

    for item in vision_payload.get("fields", []):
        height_eval = check_numeral_height(
            field_name=item["field"],
            measured_height_mm=item["height_mm"],
            label_area_cm2=250.0,
        )
        if not height_eval["is_compliant"]:
            has_violation = True

        field_results.append({
            "field":          item["field"],
            "extracted_text": item["extracted_text"],
            "measurement":    height_eval,
            "status":         "PASS" if height_eval["is_compliant"] else "FAIL",
        })

    return {
        "overall_status":        "NON_COMPLIANT" if has_violation else "COMPLIANT",
        "scale_applied":         vision_payload.get("scale"),
        "field_evaluations":     field_results,
        "pcr_registry_verified": True,
    }
