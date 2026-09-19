from typing import Any, Dict


def query_vlm(image_path: str, prompt: str) -> Dict[str, Any]:
    """Sends the image and a text prompt to a Vision-Language Model (VLM) for
    open-ended field extraction or compliance reasoning.

    Args:
        image_path: Path to the source image.
        prompt: Instruction string passed to the VLM.

    Returns:
        dict with at least:
            response – model text output
            model    – identifier of the VLM used
    """
    # STUB: Placeholder until a VLM integration (e.g. LLaVA / GPT-4V) is wired.
    return {
        "response": "STUB: VLM not yet integrated.",
        "model": "none",
    }
