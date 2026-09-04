#!/usr/bin/env python3
import contextlib
import io
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from merge_instrumentation_junit_reports import merge_reports


FINAL_ROOT = """<testsuite name="suite" tests="{tests}" failures="{failures}" errors="{errors}" skipped="0">
  {cases}
</testsuite>"""
RETRY_ROOT = """<testsuite name="suite">
  {cases}
</testsuite>"""


def testcase(name: str, classname: str = "com.example.RetryTest", result: str = "") -> str:
    child = {
        "failure": '<failure type="AssertionError">first failure</failure>',
        "error": '<error type="RuntimeException">first error</error>',
        "skipped": "<skipped />",
    }.get(result, "")
    return f'<testcase classname="{classname}" name="{name}" time="0.1">{child}</testcase>'


class InstrumentationJUnitMergeTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name) / "repo"
        self.retry = Path(self.temp_dir.name) / "retry-results"
        self.final_dir = self.root / "paymentsheet" / "build" / "instrumentation-test-results"
        self.retry_dir = self.retry / "run-1" / "attempt-1" / "paymentsheet" / "build" / "outputs" / "androidTest-results" / "managedDevice"
        self.final_dir.mkdir(parents=True)
        self.retry_dir.mkdir(parents=True)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def write_final(self, name: str, cases: str, tests: int = 1, failures: int = 0, errors: int = 0) -> Path:
        path = self.final_dir / f"TEST-{name}.xml"
        path.write_text(
            FINAL_ROOT.format(tests=tests, failures=failures, errors=errors, cases=cases),
            encoding="utf-8",
        )
        return path

    def write_retry(self, name: str, cases: str, device: str = "device-1") -> Path:
        path = self.retry_dir / device / f"TEST-{name}.xml"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(RETRY_ROOT.format(cases=cases), encoding="utf-8")
        return path

    def read_final(self, path: Path) -> ET.Element:
        return ET.parse(path).getroot()

    def test_fail_then_pass_is_flaky_and_remains_green(self) -> None:
        final = self.write_final("RetryTest", testcase("testFlaky"))
        self.write_retry("RetryTest", testcase("testFlaky", result="failure"))

        merge_reports(self.root, self.retry)

        root = self.read_final(final)
        case = root.find("testcase")
        self.assertIsNotNone(case)
        self.assertIsNotNone(case.find("flakyFailure"))
        self.assertIsNone(case.find("failure"))
        self.assertEqual("0", root.get("failures"))
        self.assertEqual("1", root.get("tests"))

    def test_repeated_failure_keeps_final_failure_and_rerun_failures(self) -> None:
        final = self.write_final("RetryTest", testcase("testAlwaysFails", result="failure"), failures=1)
        self.write_retry("RetryTest", testcase("testAlwaysFails", result="failure"))
        second = self.retry / "run-1" / "attempt-2" / "paymentsheet" / "build" / "outputs" / "androidTest-results" / "managedDevice"
        second.mkdir(parents=True)
        (second / "TEST-RetryTest.xml").write_text(
            RETRY_ROOT.format(cases=testcase("testAlwaysFails", result="failure")),
            encoding="utf-8",
        )

        merge_reports(self.root, self.retry)

        root = self.read_final(final)
        case = root.find("testcase")
        self.assertEqual(1, len(case.findall("failure")))
        self.assertEqual(2, len(case.findall("rerunFailure")))
        self.assertEqual("1", root.get("failures"))

    def test_error_then_pass_uses_flaky_error(self) -> None:
        final = self.write_final("RetryTest", testcase("testError"))
        self.write_retry("RetryTest", testcase("testError", result="error"))

        merge_reports(self.root, self.retry)

        case = self.read_final(final).find("testcase")
        self.assertIsNotNone(case.find("flakyError"))
        self.assertIsNone(case.find("error"))

    def test_parameterized_name_is_matched_exactly(self) -> None:
        parameterized_name = "testCard[US, Visa]"
        final = self.write_final("RetryTest", testcase(parameterized_name))
        self.write_retry("RetryTest", testcase(parameterized_name, result="failure"))

        merge_reports(self.root, self.retry)

        case = self.read_final(final).find("testcase")
        self.assertIsNotNone(case.find("flakyFailure"))
        self.assertEqual(parameterized_name, case.get("name"))

    def test_multi_device_reports_are_merged_by_class_and_name(self) -> None:
        first = self.write_final("FirstTest", testcase("testOne", "com.example.FirstTest"))
        second = self.final_dir / "TEST-com_example_SecondTest.xml"
        second.write_text(
            FINAL_ROOT.format(
                tests=1,
                failures=0,
                errors=0,
                cases=testcase("testTwo", "com.example.SecondTest"),
            ),
            encoding="utf-8",
        )
        self.write_retry("FirstTest", testcase("testOne", "com.example.FirstTest", "failure"), "device-1")
        self.write_retry("SecondTest", testcase("testTwo", "com.example.SecondTest", "error"), "device-2")

        merge_reports(self.root, self.retry)

        self.assertIsNotNone(self.read_final(first).find("testcase/flakyFailure"))
        self.assertIsNotNone(self.read_final(second).find("testcase/flakyError"))

    def test_unmatched_and_incomplete_reports_become_diagnostics(self) -> None:
        final = self.write_final("RetryTest", testcase("testKnown"))
        unmatched = self.write_retry("RetryTest", testcase("testMissing", result="failure"))
        incomplete = self.retry_dir / "broken.xml"
        incomplete.write_text("<testsuite><testcase", encoding="utf-8")
        stderr = io.StringIO()

        with contextlib.redirect_stderr(stderr):
            merge_reports(self.root, self.retry)

        self.assertIsNone(self.read_final(final).find("testcase/flakyFailure"))
        diagnostics = self.retry.parent / "retry-diagnostics"
        self.assertTrue((diagnostics / unmatched.relative_to(self.retry)).is_file())
        self.assertTrue((diagnostics / incomplete.relative_to(self.retry)).is_file())
        self.assertIn("no final testcase matched", stderr.getvalue())
        self.assertIn("could not parse JUnit XML", stderr.getvalue())


if __name__ == "__main__":
    unittest.main()
