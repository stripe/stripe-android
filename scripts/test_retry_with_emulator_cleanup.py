#!/usr/bin/env python3
import os
import subprocess
import tempfile
import textwrap
import time
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("retry_with_emulator_cleanup.sh")


class RetryWithEmulatorCleanupProcessTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dirs: list[tempfile.TemporaryDirectory[str]] = []

    def tearDown(self) -> None:
        for temp_dir in self.temp_dirs:
            temp_dir.cleanup()

    def run_wrapper(self, command: str, retries: int = 2, timeout: float = 8) -> tuple[subprocess.CompletedProcess[str], Path]:
        temp_dir = tempfile.TemporaryDirectory()
        self.temp_dirs.append(temp_dir)
        root = Path(temp_dir.name)
        fake_adb = root / "adb"
        fake_adb.write_text("#!/bin/sh\nprintf 'List of devices attached\\n'\n", encoding="utf-8")
        fake_adb.chmod(0o755)
        env = os.environ.copy()
        env["PATH"] = f"{root}:{env['PATH']}"
        env["BITRISE_SOURCE_DIR"] = str(root)
        env["BITRISE_RETRY_RESULTS_DIR"] = str(root / "retry-results")
        env["BITRISE_FAIL_FAST_GRACE_SECONDS"] = "1"
        started = time.monotonic()
        result = subprocess.run(
            ["bash", str(SCRIPT), "--fail-fast-tests", str(retries), "bash", "-c", textwrap.dedent(command)],
            capture_output=True,
            text=True,
            env=env,
            timeout=timeout,
        )
        result.elapsed = time.monotonic() - started
        return result, root

    def assert_process_stopped(self, pid_file: Path) -> None:
        pid = int(pid_file.read_text(encoding="utf-8"))
        for _ in range(20):
            try:
                os.kill(pid, 0)
            except ProcessLookupError:
                return
            time.sleep(0.05)
        self.fail(f"process {pid} is still running")

    def test_intermediate_test_failure_is_terminated_and_retried(self) -> None:
        result, root = self.run_wrapper(
            """
            attempt_file="$BITRISE_SOURCE_DIR/attempt"
            attempt=$(($(cat "$attempt_file" 2>/dev/null || printf 0) + 1))
            printf '%s' "$attempt" > "$attempt_file"
            if [ "$attempt" -eq 1 ]; then
              printf '%s\n' 'There were failing tests. See the report at: file:///tmp/report'
              sleep 30 &
              child=$!
              printf '%s' "$child" > "$BITRISE_SOURCE_DIR/child-pid"
              wait "$child"
            fi
            printf '%s\n' complete >> "$BITRISE_SOURCE_DIR/completions"
            """
        )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertLess(result.elapsed, 5, result.stdout)
        self.assertEqual("2", (root / "attempt").read_text(encoding="utf-8"))
        self.assertEqual("complete\n", (root / "completions").read_text(encoding="utf-8"))
        self.assertIn("Stopping failed Gradle test process group", result.stdout)
        self.assert_process_stopped(root / "child-pid")

    def test_intermediate_junit_results_are_captured_before_retry(self) -> None:
        result, root = self.run_wrapper(
            """
            attempt_file="$BITRISE_SOURCE_DIR/attempt"
            attempt=$(($(cat "$attempt_file" 2>/dev/null || printf 0) + 1))
            printf '%s' "$attempt" > "$attempt_file"
            result_dir="$BITRISE_SOURCE_DIR/paymentsheet/build/outputs/androidTest-results/managedDevice/device-1"
            mkdir -p "$result_dir"
            if [ "$attempt" -eq 1 ]; then
              printf '%s' '<testsuite><testcase classname="com.example.RetryTest" name="testFlaky"><failure>first failure</failure></testcase></testsuite>' > "$result_dir/TEST-RetryTest.xml"
              printf '%s\n' 'There were failing tests. See the report at: file:///tmp/report'
              sleep 0.3
            else
              printf '%s' '<testsuite><testcase classname="com.example.RetryTest" name="testFlaky" /></testsuite>' > "$result_dir/TEST-RetryTest.xml"
            fi
            """
        )

        self.assertEqual(0, result.returncode, result.stderr)
        captured = [
            path
            for path in (root / "retry-results").rglob("TEST-RetryTest.xml")
            if "attempt-1" in path.parts
        ]
        self.assertEqual(1, len(captured))
        self.assertIn("first failure", captured[0].read_text(encoding="utf-8"))

    def test_final_attempt_runs_to_completion(self) -> None:
        result, root = self.run_wrapper(
            """
            printf '%s\n' 'There were failing tests. See the report at: file:///tmp/report'
            sleep 0.4
            printf '%s\n' complete > "$BITRISE_SOURCE_DIR/final-complete"
            exit 0
            """,
            retries=1,
        )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue((root / "final-complete").is_file())
        self.assertGreater(result.elapsed, 0.3, result.stdout)
        self.assertNotIn("Stopping failed Gradle test process group", result.stdout)

    def test_uncooperative_process_is_force_killed_after_grace_period(self) -> None:
        result, root = self.run_wrapper(
            """
            attempt_file="$BITRISE_SOURCE_DIR/attempt"
            attempt=$(($(cat "$attempt_file" 2>/dev/null || printf 0) + 1))
            printf '%s' "$attempt" > "$attempt_file"
            if [ "$attempt" -eq 1 ]; then
              trap '' TERM
              printf '%s\n' 'There were failing tests. See the report at: file:///tmp/report'
              sleep 30 &
              child=$!
              printf '%s' "$child" > "$BITRISE_SOURCE_DIR/child-pid"
              wait "$child"
            fi
            printf '%s\n' complete >> "$BITRISE_SOURCE_DIR/completions"
            """
        )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertLess(result.elapsed, 5, result.stdout)
        self.assertIn("sending SIGKILL", result.stdout)
        self.assert_process_stopped(root / "child-pid")

    def test_non_test_failure_is_not_terminated_early(self) -> None:
        result, root = self.run_wrapper(
            """
            printf '%s\n' 'Execution failed for task :compileDebugKotlin.'
            sleep 0.4
            printf '%s\n' complete >> "$BITRISE_SOURCE_DIR/completions"
            exit 17
            """
        )

        self.assertEqual(17, result.returncode)
        self.assertEqual("complete\ncomplete\n", (root / "completions").read_text(encoding="utf-8"))
        self.assertGreater(result.elapsed, 0.7, result.stdout)
        self.assertNotIn("Stopping failed Gradle test process group", result.stdout)

    def test_final_exit_status_is_preserved(self) -> None:
        result, _ = self.run_wrapper(
            """
            exit 23
            """,
            retries=1,
        )

        self.assertEqual(23, result.returncode, result.stdout)


if __name__ == "__main__":
    unittest.main()
