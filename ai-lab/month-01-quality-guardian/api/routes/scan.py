from __future__ import annotations

import pathlib
from dataclasses import asdict

from fastapi import APIRouter, HTTPException

# ---------------------------------------------------------------------------
# Fixed, controlled demo-project path.
#
# Resolved once at import time from this file's location so it is completely
# independent of the request content.  The API never accepts filesystem paths
# from callers.
#
# This file:          month-01-quality-guardian/api/routes/scan.py
# Demo project:       month-01-quality-guardian/api/demo_project/
# ---------------------------------------------------------------------------
_DEMO_PROJECT: pathlib.Path = pathlib.Path(__file__).parent.parent / "demo_project"

# ---------------------------------------------------------------------------
# Import the scanner.  By the time this module is imported, api/main.py has
# already inserted "src/" into sys.path, so this import resolves correctly.
# ---------------------------------------------------------------------------
from quality_guardian.guardian import scan_project  # noqa: E402

router = APIRouter(tags=["Scanner"])

# Human-readable notes attached to every demo-scan response.
_DEMO_NOTES: dict[str, str] = {
    "REQUIRED_FILE": (
        "The demo project is not a Spring Boot application, so pom.xml and "
        "src/main/resources/application.properties are intentionally absent. "
        "These REQUIRED_FILE warnings are expected and do not indicate a "
        "problem with the demo or the scanner."
    ),
    "SECRET_ASSIGNMENT": (
        "The demo project contains a synthetic, non-functional API key entry "
        "to demonstrate detection capability.  The actual key value is never "
        "stored or returned — only the file path and line number are reported."
    ),
}


@router.get("/info")
def info() -> dict:
    """
    Metadata about the Quality Guardian Core scanner and this API.
    """
    return {
        "scanner": "Quality Guardian Core",
        "api_version": "0.1.0",
        "demo_project": "api/demo_project",
        "capabilities": [
            "SECRET_ASSIGNMENT detection (password, secret, api_key, access_token)",
            "PRIVATE_KEY detection (RSA, EC, OPENSSH)",
            "Placeholder / false-positive filtering",
            "Environment-variable reference filtering (${VAR})",
            "Required file structure validation",
            "Zero-leak reporting — secret values are always withheld",
        ],
        "supported_extensions": sorted([
            ".py", ".java", ".properties", ".yml", ".yaml",
            ".json", ".xml", ".gradle", ".kts", ".dart",
            ".js", ".ts", ".env", ".md", ".txt", ".sh",
            ".ps1", ".sql",
        ]),
        "ignored_directories": sorted([
            ".git", ".venv", "venv", "node_modules", "build",
            "target", ".gradle", "__pycache__", "reports",
            "tests", "configs_pre_fix",
        ]),
    }


@router.post("/demo/scan")
def demo_scan() -> dict:
    """
    Scan the fixed internal demo project and return Quality Guardian findings.

    **Security guarantees:**
    - The scan target is hard-coded at server startup; callers cannot supply paths.
    - Suspected secret values are never stored or returned in the response.
    - The scanner performs read-only static analysis only; no code is executed.

    **Expected findings on the demo project:**
    - `REQUIRED_FILE` (WARNING) × 2 — demo project is not a Spring Boot app.
    - `SECRET_ASSIGNMENT` (WARNING) × 1 — synthetic, non-functional API key.
    """
    try:
        findings = scan_project(_DEMO_PROJECT)
    except Exception:  # pragma: no cover — defensive; scanner is read-only
        raise HTTPException(
            status_code=500,
            detail=(
                "The scanner encountered an unexpected error. "
                "No filesystem changes were made."
            ),
        )

    serialized: list[dict] = [asdict(f) for f in findings]

    # Defensive double-check: guarantee no value field can leak through even
    # if a future version of the dataclass adds one.
    for item in serialized:
        item.pop("value", None)

    return {
        "scanned_target": "demo_project",
        "finding_count": len(serialized),
        "findings": serialized,
        "notes": _DEMO_NOTES,
    }
