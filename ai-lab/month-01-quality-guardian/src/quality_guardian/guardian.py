from __future__ import annotations

import json
import re
from dataclasses import asdict, dataclass
from pathlib import Path


@dataclass
class Finding:
    rule: str
    severity: str
    message: str
    file: str
    line: int | None = None


SECRET_PATTERNS = {
    "SECRET_ASSIGNMENT": re.compile(
        r"""(?i)(?<![a-z0-9])(?:[a-z0-9]+[._-])*[a-z0-9]*"""
        r"""(password|passwd|secret|api[_-]?key|access[_-]?token)\b"""
        r"""\s*[:=]\s*["']?([^\s"'#,;]+)"""
    ),

    "PRIVATE_KEY": re.compile(
        r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"
    ),
}

IGNORED_DIRS = {
    ".git",
    ".venv",
    "venv",
    "node_modules",
    "build",
    "target",
    ".gradle",
    "__pycache__",
    "reports",
    "tests",
    "configs_pre_fix",
}

IGNORED_FILES = {
    "compile_output.txt",
    "show_report.py",
}

TEXT_EXTENSIONS = {
    ".py",
    ".java",
    ".properties",
    ".yml",
    ".yaml",
    ".json",
    ".xml",
    ".gradle",
    ".kts",
    ".dart",
    ".js",
    ".ts",
    ".env",
    ".md",
    ".txt",
    ".sh",
    ".ps1",
    ".sql",
}

PLACEHOLDER_VALUES = {
    "",
    "changeme",
    "change_me",
    "placeholder",
    "your_secret",
    "your_api_key",
    "example",
    "dummy",
    "null",
    "none",
    "votre_password",
    "votre_secret",
    "votre_username",
    "votre_api_key",
    "votre_nom",
    "change_moi",
    "mon_secret",
    "a_definir",
    "changer_moi",
    "votremotdepassesecurise",
    "clesecretetreslongueetsecuriseeaumoins256bits123456",
}


def _is_placeholder(value: str) -> bool:
    normalized = value.strip().strip("\"'").lower()

    if normalized in PLACEHOLDER_VALUES:
        return True

    if normalized.startswith("${") and normalized.endswith("}"):
        return True

    if any(
        marker in normalized
        for marker in (
            "fake_test",
            "example_secret",
            "unique_test_value",
            "synthetic_test",
            "dummy_secret",
        )
    ):
        return True

    return False


def _remove_comments(content: str, suffix: str) -> str:
    """Remove simple full-line comments while preserving line numbers."""
    comment_prefixes = {
        ".py": ("#",),
        ".properties": ("#", "!"),
        ".yml": ("#",),
        ".yaml": ("#",),
        ".sh": ("#",),
        ".ps1": ("#",),
        ".md": ("<!--",),
    }

    prefixes = comment_prefixes.get(suffix.lower(), ())
    if not prefixes:
        return content

    lines = []

    for line in content.splitlines():
        stripped = line.lstrip()

        if any(stripped.startswith(prefix) for prefix in prefixes):
            lines.append("")
        else:
            lines.append(line)

    return "\n".join(lines)


def scan_project(root: Path) -> list[Finding]:
    """Run basic, read-only quality and security checks."""
    findings: list[Finding] = []

    if not root.exists() or not root.is_dir():
        return [
            Finding(
                rule="PROJECT_ROOT",
                severity="ERROR",
                message="Project directory does not exist.",
                file=".",
            )
        ]

    required_files = [
        "pom.xml",
        "src/main/resources/application.properties",
    ]

    for relative_path in required_files:
        if not (root / relative_path).is_file():
            findings.append(
                Finding(
                    rule="REQUIRED_FILE",
                    severity="WARNING",
                    message=f"Expected file is missing: {relative_path}",
                    file=relative_path,
                )
            )

    for file_path in root.rglob("*"):
        if not file_path.is_file():
            continue

        relative = file_path.relative_to(root)

        if any(part in IGNORED_DIRS for part in relative.parts):
            continue

        if file_path.name in IGNORED_FILES:
            continue

        if (
            file_path.suffix.lower() not in TEXT_EXTENSIONS
            and file_path.name != ".env"
        ):
            continue

        try:
            content = file_path.read_text(encoding="utf-8")
        except (OSError, UnicodeError):
            continue

        content = _remove_comments(content, file_path.suffix)

        for rule, pattern in SECRET_PATTERNS.items():
            matches = list(pattern.finditer(content))

            if rule == "SECRET_ASSIGNMENT":
                matches = [
                    match
                    for match in matches
                    if not _is_placeholder(match.group(2))
                ]

            if matches:
                first_match = matches[0]

                findings.append(
                    Finding(
                        rule=rule,
                        severity="WARNING",
                        message=(
                            "Potential hard-coded secret detected; "
                            "value withheld."
                        ),
                        file=relative.as_posix(),
                        line=content.count(
                            "\n", 0, first_match.start()
                        )
                        + 1,
                    )
                )

    return findings


def write_report(
    findings: list[Finding],
    output_path: Path,
) -> None:
    """Write findings to JSON without storing secret values."""
    output_path.parent.mkdir(parents=True, exist_ok=True)

    payload = {
        "finding_count": len(findings),
        "findings": [
            asdict(finding)
            for finding in findings
        ],
    }

    output_path.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False),
        encoding="utf-8",
    )
