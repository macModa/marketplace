"""
Quality Guardian API — Integration test suite.

Tests run against FastAPI's synchronous TestClient (backed by Starlette's
ASGI test transport).  No network connections are made; no filesystem paths
are written; no external services are contacted.

Test categories
---------------
- TestHealthEndpoint      : GET /health liveness check
- TestInfoEndpoint        : GET /api/info scanner metadata
- TestDemoScanEndpoint    : POST /api/demo/scan full scanner integration
- TestSecretRedaction     : guarantee secret values never appear in responses
- TestInvalidRequests     : wrong method, unknown routes
- TestCORSHeaders         : origin allow/deny behaviour
"""
from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

# Importing app triggers api/main.py which inserts "src/" into sys.path,
# making quality_guardian importable for all subsequent code.
from api.main import app, _ALLOWED_ORIGINS

client = TestClient(app, raise_server_exceptions=True)

# The synthetic demo key written to api/demo_project/config.properties.
# This exact string must never appear in any API response.
_DEMO_SYNTHETIC_KEY = "D3m0ApiK3yF4k3Xr9pQ2wL"


# ---------------------------------------------------------------------------
# GET /health
# ---------------------------------------------------------------------------
class TestHealthEndpoint:
    def test_returns_200(self):
        response = client.get("/health")
        assert response.status_code == 200

    def test_content_type_is_json(self):
        response = client.get("/health")
        assert "application/json" in response.headers["content-type"]

    def test_response_has_status_ok(self):
        response = client.get("/health")
        assert response.json()["status"] == "ok"

    def test_response_contains_required_fields(self):
        data = client.get("/health").json()
        for field in ("status", "service", "version", "timestamp"):
            assert field in data, f"Missing field: {field}"

    def test_timestamp_is_iso8601(self):
        data = client.get("/health").json()
        ts = data["timestamp"]
        # Must end with timezone offset or Z; basic structural check
        assert "T" in ts and ("+" in ts or ts.endswith("Z"))


# ---------------------------------------------------------------------------
# GET /api/info
# ---------------------------------------------------------------------------
class TestInfoEndpoint:
    def test_returns_200(self):
        response = client.get("/api/info")
        assert response.status_code == 200

    def test_response_has_scanner_field(self):
        data = client.get("/api/info").json()
        assert "scanner" in data
        assert "Quality Guardian" in data["scanner"]

    def test_response_lists_capabilities(self):
        data = client.get("/api/info").json()
        caps = data["capabilities"]
        assert isinstance(caps, list)
        assert len(caps) > 0

    def test_capabilities_mention_secret_detection(self):
        data = client.get("/api/info").json()
        caps_text = " ".join(data["capabilities"]).upper()
        assert "SECRET" in caps_text

    def test_capabilities_mention_private_key(self):
        data = client.get("/api/info").json()
        caps_text = " ".join(data["capabilities"]).upper()
        assert "PRIVATE_KEY" in caps_text

    def test_capabilities_mention_zero_leak(self):
        data = client.get("/api/info").json()
        caps_text = " ".join(data["capabilities"]).lower()
        assert "withheld" in caps_text or "leak" in caps_text or "zero" in caps_text

    def test_response_lists_supported_extensions(self):
        data = client.get("/api/info").json()
        exts = data["supported_extensions"]
        assert isinstance(exts, list)
        assert ".py" in exts
        assert ".java" in exts
        assert ".properties" in exts

    def test_response_lists_ignored_directories(self):
        data = client.get("/api/info").json()
        ignored = data["ignored_directories"]
        assert isinstance(ignored, list)
        assert ".venv" in ignored
        assert "node_modules" in ignored


# ---------------------------------------------------------------------------
# POST /api/demo/scan — scanner integration
# ---------------------------------------------------------------------------
class TestDemoScanEndpoint:
    def test_returns_200(self):
        response = client.post("/api/demo/scan")
        assert response.status_code == 200

    def test_content_type_is_json(self):
        response = client.post("/api/demo/scan")
        assert "application/json" in response.headers["content-type"]

    def test_response_has_required_top_level_fields(self):
        data = client.post("/api/demo/scan").json()
        for field in ("scanned_target", "finding_count", "findings", "notes"):
            assert field in data, f"Missing top-level field: {field}"

    def test_scanned_target_is_demo_project(self):
        data = client.post("/api/demo/scan").json()
        assert data["scanned_target"] == "demo_project"

    def test_finding_count_matches_findings_list_length(self):
        data = client.post("/api/demo/scan").json()
        assert data["finding_count"] == len(data["findings"])

    def test_each_finding_has_required_fields(self):
        data = client.post("/api/demo/scan").json()
        for finding in data["findings"]:
            for field in ("rule", "severity", "message", "file"):
                assert field in finding, (
                    f"Finding missing field '{field}': {finding}"
                )

    def test_detects_required_file_warning(self):
        """Demo project has no pom.xml or application.properties → REQUIRED_FILE."""
        data = client.post("/api/demo/scan").json()
        rules = {f["rule"] for f in data["findings"]}
        assert "REQUIRED_FILE" in rules

    def test_detects_secret_assignment(self):
        """Demo config.properties contains a synthetic API key → SECRET_ASSIGNMENT."""
        data = client.post("/api/demo/scan").json()
        rules = {f["rule"] for f in data["findings"]}
        assert "SECRET_ASSIGNMENT" in rules

    def test_secret_assignment_finding_points_to_config_file(self):
        data = client.post("/api/demo/scan").json()
        secret_findings = [
            f for f in data["findings"] if f["rule"] == "SECRET_ASSIGNMENT"
        ]
        assert len(secret_findings) >= 1
        assert secret_findings[0]["file"] == "config.properties"

    def test_secret_assignment_finding_has_line_number(self):
        data = client.post("/api/demo/scan").json()
        secret_findings = [
            f for f in data["findings"] if f["rule"] == "SECRET_ASSIGNMENT"
        ]
        assert secret_findings[0]["line"] is not None
        assert isinstance(secret_findings[0]["line"], int)
        assert secret_findings[0]["line"] > 0

    def test_notes_explain_required_file_warnings(self):
        data = client.post("/api/demo/scan").json()
        notes = data["notes"]
        assert "REQUIRED_FILE" in notes
        assert len(notes["REQUIRED_FILE"]) > 20

    def test_notes_explain_secret_assignment(self):
        data = client.post("/api/demo/scan").json()
        notes = data["notes"]
        assert "SECRET_ASSIGNMENT" in notes

    def test_findings_have_warning_severity(self):
        data = client.post("/api/demo/scan").json()
        for finding in data["findings"]:
            assert finding["severity"] in ("WARNING", "ERROR"), (
                f"Unexpected severity: {finding['severity']}"
            )


# ---------------------------------------------------------------------------
# Secret-value redaction guarantee
# ---------------------------------------------------------------------------
class TestSecretRedaction:
    def test_synthetic_key_absent_from_response_body(self):
        """The fake key written into config.properties must never appear."""
        response = client.post("/api/demo/scan")
        assert _DEMO_SYNTHETIC_KEY not in response.text, (
            "Synthetic demo key was found in the API response body — "
            "this is a redaction failure."
        )

    def test_secret_finding_message_says_value_withheld(self):
        """Scanner messages must state the value is withheld, not reveal it."""
        data = client.post("/api/demo/scan").json()
        for finding in data["findings"]:
            if finding["rule"] == "SECRET_ASSIGNMENT":
                assert "withheld" in finding["message"].lower(), (
                    f"Message does not say value is withheld: {finding['message']}"
                )

    def test_findings_have_no_value_field(self):
        """Findings must not carry a 'value' key under any circumstances."""
        data = client.post("/api/demo/scan").json()
        for finding in data["findings"]:
            assert "value" not in finding, (
                f"'value' key found in finding: {finding}"
            )


# ---------------------------------------------------------------------------
# Invalid requests
# ---------------------------------------------------------------------------
class TestInvalidRequests:
    def test_get_demo_scan_returns_405_method_not_allowed(self):
        """POST-only endpoint must reject GET."""
        response = client.get("/api/demo/scan")
        assert response.status_code == 405

    def test_put_demo_scan_returns_405(self):
        response = client.put("/api/demo/scan")
        assert response.status_code == 405

    def test_unknown_api_route_returns_404(self):
        response = client.get("/api/nonexistent-endpoint")
        assert response.status_code == 404

    def test_root_route_returns_404(self):
        """No root handler is registered."""
        response = client.get("/")
        assert response.status_code == 404


# ---------------------------------------------------------------------------
# CORS headers
# ---------------------------------------------------------------------------
class TestCORSHeaders:
    def test_allowed_origin_vite_dev_server_receives_acao_header(self):
        """Requests from the Vite dev origin must get the ACAO header."""
        response = client.get(
            "/health",
            headers={"Origin": "http://localhost:5173"},
        )
        assert response.status_code == 200
        acao = response.headers.get("access-control-allow-origin", "")
        assert acao == "http://localhost:5173", (
            f"Expected ACAO 'http://localhost:5173', got '{acao}'"
        )

    def test_allowed_origin_localhost_3000_receives_acao_header(self):
        response = client.get(
            "/health",
            headers={"Origin": "http://localhost:3000"},
        )
        assert response.status_code == 200
        acao = response.headers.get("access-control-allow-origin", "")
        assert acao == "http://localhost:3000"

    def test_unknown_origin_does_not_receive_wildcard_acao(self):
        """Disallowed origins must not get a wildcard or matching ACAO header."""
        response = client.get(
            "/health",
            headers={"Origin": "https://attacker.example.com"},
        )
        acao = response.headers.get("access-control-allow-origin", "")
        assert acao != "*", "CORS must not use wildcard for unknown origins."
        assert "attacker.example.com" not in acao

    def test_preflight_from_allowed_origin_returns_200(self):
        """OPTIONS preflight from an allowed origin must succeed."""
        response = client.options(
            "/health",
            headers={
                "Origin": "http://localhost:5173",
                "Access-Control-Request-Method": "GET",
            },
        )
        assert response.status_code == 200
        assert "access-control-allow-origin" in response.headers

    def test_allowed_origins_list_contains_expected_entries(self):
        """Sanity check: the origins list has been populated correctly."""
        assert "http://localhost:5173" in _ALLOWED_ORIGINS
        assert "http://localhost:3000" in _ALLOWED_ORIGINS
