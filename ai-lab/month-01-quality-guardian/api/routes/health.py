from __future__ import annotations

from datetime import datetime, timezone

from fastapi import APIRouter

router = APIRouter(tags=["Monitoring"])

_VERSION = "0.1.0"
_SERVICE = "quality-guardian-api"


@router.get("/health")
def health() -> dict:
    """
    Liveness check.  Returns HTTP 200 while the service is running.
    Suitable for Render health-check pings and frontend status indicators.
    """
    return {
        "status": "ok",
        "service": _SERVICE,
        "version": _VERSION,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }
