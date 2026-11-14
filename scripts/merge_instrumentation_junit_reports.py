#!/usr/bin/env python3
"""Merge failed intermediate instrumentation attempts into final JUnit reports."""
from __future__ import annotations

import argparse
import copy
import os
import re
import shutil
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path


DEFAULT_RETRY_RESULTS = Path("/tmp/test_results/retry-results")


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--source-dir",
        type=Path,
        default=Path(os.environ.get("BITRISE_SOURCE_DIR", ".")),
        help="Repository containing final build/instrumentation-test-results directories.",
    )
    parser.add_argument(
        "--retry-results",
        type=Path,
        default=Path(os.environ.get("BITRISE_RETRY_RESULTS_DIR", DEFAULT_RETRY_RESULTS)),
        help="Directory containing retry run snapshots.",
    )
    parser.add_argument(
        "--diagnostics-dir",
        type=Path,
        default=None,
        help="Directory for unmatched or incomplete retry reports.",
    )
    return parser.parse_args()


def suite_elements(root: ET.Element) -> list[ET.Element]:
    root_tag = local_name(root.tag)
    if root_tag == "testsuite":
        return [root]
    if root_tag == "testsuites":
        return [element for element in root.iter() if local_name(element.tag) == "testsuite"]
    return []


def suite_class_name(suite: ET.Element) -> str | None:
    for testcase in suite:
        if local_name(testcase.tag) == "testcase" and testcase.get("classname"):
            return testcase.get("classname")
    return suite.get("name") or None


def testcase_elements(suite: ET.Element) -> list[ET.Element]:
    return [element for element in suite if local_name(element.tag) == "testcase"]


def testcase_key(testcase: ET.Element, fallback_classname: str | None) -> tuple[str, str] | None:
    classname = testcase.get("classname") or fallback_classname
    name = testcase.get("name")
    if not classname or not name:
        return None
    return classname, name


def failure_elements(testcase: ET.Element) -> list[ET.Element]:
    return [
        element
        for element in testcase
        if local_name(element.tag) in {"failure", "error"}
    ]


@dataclass
class EarlyFailure:
    module: str
    key: tuple[str, str]
    attempt: int
    source: Path
    element: ET.Element
    matched: bool = False


@dataclass
class MergeState:
    warnings: dict[Path, list[str]] = field(default_factory=lambda: defaultdict(list))
    failures: list[EarlyFailure] = field(default_factory=list)

    def warn(self, source: Path, message: str) -> None:
        self.warnings[source].append(message)


def module_for_build_path(path: Path) -> str | None:
    parts = path.parts
    try:
        build_index = parts.index("build")
    except ValueError:
        return None
    module_parts = parts[:build_index]
    attempt_indices = [
        index
        for index, part in enumerate(module_parts)
        if re.fullmatch(r"attempt-\d+", part)
    ]
    if attempt_indices:
        module_parts = module_parts[attempt_indices[-1] + 1 :]
    module = Path(*module_parts)
    return str(module) if str(module) else "."


def attempt_number(path: Path) -> int | None:
    for part in path.parts:
        match = re.fullmatch(r"attempt-(\d+)", part)
        if match:
            return int(match.group(1))
    return None


def retry_xml_files(retry_results: Path) -> list[Path]:
    if not retry_results.is_dir():
        return []
    paths = [
        path
        for run_dir in retry_results.glob("run-*")
        for attempt_dir in run_dir.glob("attempt-*")
        for path in attempt_dir.rglob("*.xml")
        if path.is_file()
    ]
    return sorted(
        paths,
        key=lambda path: (
            next(part for part in path.parts if part.startswith("run-")),
            attempt_number(path) or 0,
            str(path),
        ),
    )


def read_early_failures(retry_results: Path, state: MergeState) -> None:
    for path in retry_xml_files(retry_results):
        attempt = attempt_number(path)
        if attempt is None:
            state.warn(path, "retry snapshot is missing an attempt number")
            continue

        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as error:
            state.warn(path, f"could not parse JUnit XML: {error}")
            continue

        suites = suite_elements(root)
        if not suites:
            state.warn(path, "XML does not contain a testsuite or testsuites root")
            continue

        relative = path.relative_to(retry_results)
        module = module_for_build_path(relative)
        if module is None:
            state.warn(path, "retry result path does not identify a module")
            continue

        found_case = False
        found_failure = False
        for suite in suites:
            fallback_classname = suite_class_name(suite)
            for testcase in testcase_elements(suite):
                found_case = True
                failures = failure_elements(testcase)
                if not failures:
                    continue
                found_failure = True
                key = testcase_key(testcase, fallback_classname)
                if key is None:
                    state.warn(path, "failing testcase is missing classname or name")
                    continue
                for failure in failures:
                    state.failures.append(
                        EarlyFailure(
                            module=module,
                            key=key,
                            attempt=attempt,
                            source=path,
                            element=copy.deepcopy(failure),
                        )
                    )

        if not found_case:
            state.warn(path, "JUnit report contains no testcases")
        elif not found_failure:
            state.warn(path, "intermediate retry report contains no failing testcases")


def final_report_files(source_root: Path) -> list[Path]:
    if not source_root.is_dir():
        return []
    return sorted(
        path
        for result_dir in source_root.rglob("instrumentation-test-results")
        if result_dir.is_dir()
        for path in result_dir.glob("*.xml")
        if path.is_file()
    )


def final_status(testcase: ET.Element) -> str:
    child_tags = {local_name(element.tag) for element in testcase}
    if "error" in child_tags:
        return "error"
    if "failure" in child_tags:
        return "failure"
    if "skipped" in child_tags:
        return "skipped"
    return "pass"


def annotation_tag(early_tag: str, final: str) -> str:
    suffix = "Error" if early_tag == "error" else "Failure"
    prefix = "flaky" if final == "pass" else "rerun"
    return prefix + suffix


def update_suite_counts(suite: ET.Element) -> None:
    cases = testcase_elements(suite)
    statuses = [final_status(testcase) for testcase in cases]
    suite.set("tests", str(len(cases)))
    suite.set("failures", str(statuses.count("failure")))
    suite.set("errors", str(statuses.count("error")))
    suite.set("skipped", str(statuses.count("skipped")))


def update_testsuites_counts(root: ET.Element) -> None:
    if local_name(root.tag) != "testsuites":
        return
    suites = suite_elements(root)
    root.set("tests", str(sum(int(suite.get("tests", "0")) for suite in suites)))
    root.set("failures", str(sum(int(suite.get("failures", "0")) for suite in suites)))
    root.set("errors", str(sum(int(suite.get("errors", "0")) for suite in suites)))
    root.set("skipped", str(sum(int(suite.get("skipped", "0")) for suite in suites)))


def merge_final_report(
    path: Path,
    source_root: Path,
    failures_by_key: dict[tuple[str, tuple[str, str]], list[EarlyFailure]],
) -> int:
    tree = ET.parse(path)
    root = tree.getroot()
    relative = path.relative_to(source_root)
    module = module_for_build_path(relative)
    if module is None:
        return 0

    annotations = 0
    for suite in suite_elements(root):
        fallback_classname = suite_class_name(suite)
        for testcase in testcase_elements(suite):
            key = testcase_key(testcase, fallback_classname)
            if key is None:
                continue
            early_failures = failures_by_key.get((module, key), [])
            if not early_failures:
                continue
            current_status = final_status(testcase)
            if current_status == "skipped":
                continue
            for early_failure in early_failures:
                annotated = copy.deepcopy(early_failure.element)
                annotated.tag = annotation_tag(local_name(annotated.tag), current_status)
                testcase.append(annotated)
                early_failure.matched = True
                annotations += 1
        update_suite_counts(suite)
    update_testsuites_counts(root)
    if annotations:
        ET.indent(tree, space="  ")
        tree.write(path, encoding="utf-8", xml_declaration=True)
    return annotations


def copy_diagnostics(
    retry_results: Path,
    diagnostics_dir: Path,
    state: MergeState,
) -> None:
    for source, reasons in state.warnings.items():
        try:
            relative = source.relative_to(retry_results)
        except ValueError:
            relative = Path(source.name)
        destination = diagnostics_dir / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
        warning_file = destination.with_name(destination.name + ".warning.txt")
        warning_file.write_text("\n".join(reasons) + "\n", encoding="utf-8")
        for reason in reasons:
            print(f"Warning: {source}: {reason}", file=sys.stderr)


def merge_reports(
    source_root: Path,
    retry_results: Path,
    diagnostics_dir: Path | None = None,
) -> int:
    source_root = source_root.resolve()
    retry_results = retry_results.resolve()
    if diagnostics_dir is None:
        diagnostics_dir = retry_results.parent / "retry-diagnostics"
    diagnostics_dir = diagnostics_dir.resolve()

    state = MergeState()
    read_early_failures(retry_results, state)
    failures_by_key: dict[tuple[str, tuple[str, str]], list[EarlyFailure]] = defaultdict(list)
    for failure in state.failures:
        failures_by_key[(failure.module, failure.key)].append(failure)

    annotations = 0
    for path in final_report_files(source_root):
        try:
            annotations += merge_final_report(path, source_root, failures_by_key)
        except (ET.ParseError, ValueError) as error:
            state.warn(path, f"could not update final JUnit XML: {error}")

    for failure in state.failures:
        if not failure.matched:
            state.warn(
                failure.source,
                f"no final testcase matched {failure.key[0]}#{failure.key[1]} from attempt {failure.attempt}",
            )

    if state.warnings:
        copy_diagnostics(retry_results, diagnostics_dir, state)
    if state.failures:
        print(f"Merged {annotations} intermediate instrumentation result(s).")
    elif not retry_results.is_dir():
        print(f"No retry result snapshots found at {retry_results}.")
    return annotations


def main() -> None:
    args = parse_args()
    merge_reports(args.source_dir, args.retry_results, args.diagnostics_dir)


if __name__ == "__main__":
    main()
