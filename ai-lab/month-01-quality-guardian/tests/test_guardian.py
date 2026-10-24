
import json
import tempfile
import unittest
from pathlib import Path

from src.quality_guardian.guardian import scan_project, write_report


class QualityGuardianTests(unittest.TestCase):
    def test_missing_required_files_are_reported(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            findings = scan_project(Path(temporary_directory))

        rules = {finding.rule for finding in findings}
        self.assertIn("REQUIRED_FILE", rules)

    def test_secret_value_is_not_in_report(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            test_value = "R8pL2xK9mQ4vZ7"

            (root / "config.properties").write_text(
                "app.password=" + test_value + "\n",
                encoding="utf-8",
            )

            findings = scan_project(root)
            report = root / "reports" / "report.json"
            write_report(findings, report)

            report_text = report.read_text(encoding="utf-8")
            report_data = json.loads(report_text)
            rules = {
                finding["rule"]
                for finding in report_data["findings"]
            }

            self.assertIn("SECRET_ASSIGNMENT", rules)
            self.assertNotIn(test_value, report_text)

    def test_environment_variable_placeholder_is_ignored(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)

            (root / "config.properties").write_text(
                "app.password=${DB_PASSWORD}\n",
                encoding="utf-8",
            )

            findings = scan_project(root)
            secret_findings = [
                finding for finding in findings
                if finding.rule == "SECRET_ASSIGNMENT"
            ]

            self.assertEqual(secret_findings, [])

    def test_secret_line_number_is_preserved_after_comment(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            resources = root / "src" / "main" / "resources"
            resources.mkdir(parents=True)

            (root / "pom.xml").write_text(
                "<project></project>",
                encoding="utf-8",
            )
            (resources / "application.properties").write_text(
                "# commentaire\n"
                "\n"
                "api_key=R8pL2xK9mQ4vZ7\n",
                encoding="utf-8",
            )

            findings = scan_project(root)
            secret_findings = [
                finding for finding in findings
                if finding.rule == "SECRET_ASSIGNMENT"
            ]

            self.assertEqual(len(secret_findings), 1)
            self.assertEqual(secret_findings[0].line, 3)


    def test_french_placeholder_is_ignored(self):
        """French doc placeholders like 'votre_password' must not trigger alerts."""
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)

            (root / "config.yml").write_text(
                "spring:\n  datasource:\n    password: votre_password\n",
                encoding="utf-8",
            )

            findings = scan_project(root)
            secret_findings = [
                finding for finding in findings
                if finding.rule == "SECRET_ASSIGNMENT"
            ]

            self.assertEqual(secret_findings, [])


    def test_prefixed_api_key_is_detected(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)

            (root / "config.properties").write_text(
                "TEST_API_KEY=Q7mR4pL9vX2nK8\n",
                encoding="utf-8",
            )

            findings = scan_project(root)
            secret_findings = [
                finding for finding in findings
                if finding.rule == "SECRET_ASSIGNMENT"
            ]

            self.assertEqual(len(secret_findings), 1)


    def test_documentation_secret_placeholders_are_ignored(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)

            (root / "README.md").write_text(
                "DB_PASSWORD=VotreMotDePasseSecurise\n"
                "JWT_SECRET=CleSecreteTresLongueEtSecuriseeAuMoins256Bits123456\n",
                encoding="utf-8",
            )

            findings = scan_project(root)
            readme_findings = [
                finding for finding in findings
                if finding.file == "README.md"
                and finding.rule == "SECRET_ASSIGNMENT"
            ]

            self.assertEqual(readme_findings, [])
    def test_private_key_marker_is_detected(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)

            (root / "private_key.txt").write_text(
                "-----BEGIN RSA PRIVATE KEY-----\n",
                encoding="utf-8",
            )

            findings = scan_project(root)
            private_key_findings = [
                finding
                for finding in findings
                if finding.rule == "PRIVATE_KEY"
            ]

            self.assertEqual(len(private_key_findings), 1)

    def test_camel_case_secret_is_detected(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)

            (root / "JwtTokenProvider.java").write_text(
                'String jwtSecret = "Q7mR4pL9vX2nK8";\n',
                encoding="utf-8",
            )

            findings = scan_project(root)
            secret_findings = [
                finding for finding in findings
                if finding.rule == "SECRET_ASSIGNMENT"
            ]

            self.assertEqual(len(secret_findings), 1)
            self.assertEqual(
                secret_findings[0].file,
                "JwtTokenProvider.java",
            )

if __name__ == "__main__":
    unittest.main()
