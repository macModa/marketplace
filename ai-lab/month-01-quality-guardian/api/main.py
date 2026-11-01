from __future__ import annotations

# ---------------------------------------------------------------------------
# sys.path setup — must run before any local package imports.
#
# This file lives at:   month-01-quality-guardian/api/main.py
# The scanner lives at: month-01-quality-guardian/src/quality_guardian/
#
# Adding "src/" to sys.path makes `import quality_guardian.guardian` work
# without installing the scanner as a package, preserving its stdlib-only
# nature and the existing project layout.
# ---------------------------------------------------------------------------
import os
import pathlib
import sys

_MODULE_DIR = pathlib.Path(__file__).parent          # .../api/
_PROJECT_ROOT = _MODULE_DIR.parent                   # .../month-01-quality-guardian/
_SRC_DIR = _PROJECT_ROOT / "src"

if str(_SRC_DIR) not in sys.path:
    sys.path.insert(0, str(_SRC_DIR))

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .routes.health import router as health_router
from .routes.scan import router as scan_router

# ---------------------------------------------------------------------------
# Allowed CORS origins.
# Add the deployed Vercel URL via the FRONTEND_ORIGIN environment variable
# before deploying to production.  Never use allow_origins=["*"] for an
# authenticated or commercially deployed service.
# ---------------------------------------------------------------------------
_ALLOWED_ORIGINS: list[str] = [
    "http://localhost:5173",   # Vite dev server default
    "http://localhost:3000",   # Alternative local dev port
]

_frontend_origin = os.getenv("FRONTEND_ORIGIN", "").strip()
if _frontend_origin:
    _ALLOWED_ORIGINS.append(_frontend_origin)

# ---------------------------------------------------------------------------
# Application
# ---------------------------------------------------------------------------
app = FastAPI(
    title="Quality Guardian API",
    description=(
        "Read-only static analysis API powered by the Quality Guardian Core "
        "scanner. Detects potential hard-coded secrets and structural issues "
        "in source projects. Secret values are never stored or returned."
    ),
    version="0.1.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=_ALLOWED_ORIGINS,
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)

app.include_router(health_router)
app.include_router(scan_router, prefix="/api")
