import argparse
import json
import sys
from pathlib import Path

from src.quality_guardian.guardian import scan_project, write_report


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Quality Guardian - read-only project checks"
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    scan_parser = subparsers.add_parser(
        "scan", help="Scan a project and write a JSON report"
    )
    scan_parser.add_argument(
        "--root",
        type=Path,
        default=Path.cwd().parents[1],
        help="Project root to scan (default: Marketplace root)",
    )
    scan_parser.add_argument(
        "--report",
        type=Path,
        default=Path("reports/quality-report.json"),
        help="Path to the JSON report",
    )
    scan_parser.add_argument(
        "--fail-on-warning",
        action="store_true",
        help="Return exit code 1 if warnings are found",
    )

    args = parser.parse_args()

    if args.command == "scan":
        root = args.root.resolve()
        report = args.report.resolve()
        findings = scan_project(root)
        write_report(findings, report)

        errors = sum(item.severity == "ERROR" for item in findings)
        warnings = sum(item.severity == "WARNING" for item in findings)

        print(f"Scanned project: {root}")
        print(f"Findings: {len(findings)} (errors={errors}, warnings={warnings})")
        print(f"JSON report: {report}")

        if errors or (args.fail_on_warning and warnings):
            return 1

        return 0

    return 2


if __name__ == "__main__":
    sys.exit(main())
