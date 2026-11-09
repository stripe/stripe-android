import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET


SCRIPTS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("retry", SCRIPTS / "instrumentation_test_retry.py")
retry = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(retry)


class InstrumentationTestRetryTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        self.project = self.root / "example"
        self.results = self.project / "build/outputs/androidTest-results/managedDevice/debug/device"
        self.state = {}

    def snapshot(self):
        retry.snapshot(self.state, self.root, ["./gradlew", ":example:deviceDebugAndroidTest"])

    def report(self, cases, filename="TEST-device.xml", directory=None):
        directory = directory or self.results
        directory.mkdir(parents=True, exist_ok=True)
        suite = ET.Element("testsuite", tests=str(len(cases)))
        for name, outcome in cases:
            case = ET.SubElement(suite, "testcase", classname="example.Tests", name=name, time="1.5")
            if outcome:
                ET.SubElement(case, outcome)
        path = directory / filename
        ET.ElementTree(suite).write(path)
        return path

    def completed(self, project=None):
        return set(self.state["completed"].get(str(project or self.project), {}))

    def test_only_successful_cases_are_excluded_from_retries(self):
        self.snapshot()
        self.report([("passes", None), ("fails", "failure"), ("errors", "error"), ("skips", "skipped")])

        retry.update(self.state, self.root)

        self.assertEqual(self.completed(), {"example.Tests#passes"})

    def test_reports_from_before_the_command_do_not_exclude_tests(self):
        self.report([("stalePass", None)])
        self.snapshot()

        retry.update(self.state, self.root)

        self.assertEqual(self.completed(), set())

    def test_successes_accumulate_as_the_remaining_tests_pass(self):
        self.snapshot()
        self.report([("first", None), ("second", "failure"), ("third", "failure")])
        retry.update(self.state, self.root)
        self.snapshot()
        self.report([("second", None), ("third", "failure")])

        retry.update(self.state, self.root)

        self.assertEqual(self.completed(), {"example.Tests#first", "example.Tests#second"})

    def test_interrupted_run_does_not_exclude_unreported_tests(self):
        self.snapshot()
        self.report([("finishedBeforeDisconnect", None), ("failedBeforeDisconnect", "failure")])

        retry.update(self.state, self.root)

        self.assertEqual(self.completed(), {"example.Tests#finishedBeforeDisconnect"})
        self.assertNotIn("example.Tests#neverStarted", self.completed())

    def test_exclusions_are_scoped_to_the_project(self):
        other = self.root / "other"
        self.snapshot()
        self.report([("sameName", None)])
        self.report([("sameName", "failure")], directory=other / "build/outputs/androidTest-results/connected/debug")

        retry.update(self.state, self.root)

        self.assertEqual(self.completed(), {"example.Tests#sameName"})
        self.assertEqual(self.completed(other), set())

    def test_failure_on_another_device_overrides_a_success(self):
        self.snapshot()
        self.report([("onMultipleDevices", None)], filename="TEST-first.xml")
        self.report([("onMultipleDevices", "failure")], filename="TEST-second.xml")

        retry.update(self.state, self.root)

        self.assertEqual(self.completed(), set())

    def test_success_on_another_device_does_not_override_a_failure(self):
        self.snapshot()
        self.report([("onMultipleDevices", "failure")], filename="TEST-first.xml")
        self.report([("onMultipleDevices", None)], filename="TEST-second.xml")

        retry.update(self.state, self.root)

        self.assertEqual(self.completed(), set())

    def test_parameterized_exclusion_keeps_the_exact_case_name(self):
        self.snapshot()
        self.report([("parameterized[0]", None), ("parameterized[1]", "failure")])

        retry.update(self.state, self.root)

        self.assertEqual(self.completed(), {"example.Tests#parameterized[0]"})

    def test_unrepresentable_parameterized_names_remain_eligible(self):
        self.snapshot()
        self.report([("parameterized[first,second]", None)])

        retry.update(self.state, self.root)

        self.assertEqual(self.completed(), set())

    def test_malformed_and_missing_reports_do_not_add_exclusions(self):
        self.snapshot()
        path = self.report([])
        path.write_text("<testsuite><testcase")

        retry.update(self.state, self.root)

        self.assertEqual(self.completed(), set())

    def test_finished_reports_retain_earlier_successes_without_duplicates(self):
        self.snapshot()
        self.report([("first", None), ("second", "failure")])
        retry.update(self.state, self.root)
        self.snapshot()
        self.report([("second", None)])
        retry.update(self.state, self.root)

        retry.finish(self.state)

        archive = self.project / "build/outputs/androidTest-results/retries" / f"TEST-{self.state['scope']}.xml"
        self.assertEqual([case.get("name") for case in ET.parse(archive).getroot()], ["first"])
        self.assertEqual([case.get("name") for case in retry.read_cases(self.results / "TEST-device.xml")], ["second"])

    def test_missing_reports_on_retry_do_not_duplicate_the_previous_report(self):
        self.snapshot()
        self.report([("first", None), ("second", "failure")])
        retry.update(self.state, self.root)
        self.snapshot()
        retry.update(self.state, self.root)

        retry.finish(self.state)

        self.assertFalse((self.project / "build/outputs/androidTest-results/retries").exists())

    def test_new_invocation_replaces_only_its_own_archive(self):
        self.snapshot()
        archive_dir = self.project / "build/outputs/androidTest-results/retries"
        archive_dir.mkdir(parents=True)
        previous = archive_dir / f"TEST-{self.state['scope']}.xml"
        other = archive_dir / "TEST-other-command.xml"
        previous.write_text("<testsuite />")
        other.write_text("<testsuite />")
        self.state = {}

        self.snapshot()

        self.assertFalse(previous.exists())
        self.assertTrue(other.exists())

    def test_wrapper_stops_after_filtered_retry_succeeds(self):
        result, invocations = self.run_wrapper(fail_forever=False)

        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(invocations, [[], ["example.Tests#passes"]])
        archives = list((self.project / "build/outputs/androidTest-results/retries").glob("*.xml"))
        self.assertEqual(len(archives), 1)
        self.assertEqual([case.get("name") for case in ET.parse(archives[0]).getroot()], ["passes"])

    def test_wrapper_preserves_failure_when_retries_are_exhausted(self):
        result, invocations = self.run_wrapper(fail_forever=True)

        self.assertEqual(result.returncode, 7, result.stdout + result.stderr)
        self.assertEqual(invocations, [[], ["example.Tests#passes"], ["example.Tests#passes"]])

    def run_wrapper(self, fail_forever):
        bin_dir = self.root / "bin"
        bin_dir.mkdir()
        adb = bin_dir / "adb"
        adb.write_text("#!/bin/sh\nprintf 'List of devices attached\\n'\n")
        adb.chmod(0o755)
        command = self.root / "fake_gradle.py"
        command.write_text('''import json, os, pathlib, sys
root = pathlib.Path.cwd()
state = json.loads(pathlib.Path(os.environ['STRIPE_INSTRUMENTATION_RETRY_STATE']).read_text())
passed = state['completed'].get(str(root / 'example'), {})
with (root / 'invocations.jsonl').open('a') as log:
    log.write(json.dumps(sorted(passed)) + '\\n')
results = root / 'example/build/outputs/androidTest-results/managedDevice/debug/device'
results.mkdir(parents=True, exist_ok=True)
first = not passed
failed = first or sys.argv[1] == 'true'
cases = '<testcase classname="example.Tests" name="passes" />' if first else ''
cases += '<testcase classname="example.Tests" name="fails">' + ('<failure />' if failed else '') + '</testcase>'
(results / 'TEST-device.xml').write_text('<testsuite>' + cases + '</testsuite>')
sys.exit(7 if failed else 0)
''')
        env = dict(os.environ, PATH=str(bin_dir) + os.pathsep + os.environ["PATH"])
        result = subprocess.run(
            ["bash", str(SCRIPTS / "retry_with_emulator_cleanup.sh"), "3", sys.executable,
             str(command), str(fail_forever).lower()],
            cwd=self.root, env=env, text=True, capture_output=True, timeout=30,
        )
        invocations = [json.loads(line) for line in (self.root / "invocations.jsonl").read_text().splitlines()]
        return result, invocations


if __name__ == "__main__":
    unittest.main()
