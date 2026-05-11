#!/usr/bin/env python3
"""Merge Maestro JUnit try-reports and publish for Bitrise Test Reports."""
from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

DEFAULT_STAGING = Path("/tmp/test_results")


def merged_junit_path(staging: Path, module: str) -> Path:
    return staging / f"{module}.xml"


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--tag", required=True, help="JUnit testsuite name (e.g. Maestro tag)")
    p.add_argument("--module", required=True, help="Module label for paths and test-info.json")
    p.add_argument(
        "--staging",
        type=Path,
        default=DEFAULT_STAGING,
        help="Directory containing per-attempt JUnit XML (*-try*.xml)",
    )
    return p.parse_args()


def iter_try_junit_files(staging: Path, merged_report: Path) -> list[Path]:
    """Maestro outputs *-tryN.xml per attempt."""
    merged_res = merged_report.resolve()
    return sorted(
        (p for p in staging.glob("*-try*.xml") if p.is_file() and p.resolve() != merged_res),
        key=lambda x: x.name,
    )


def collect_testcases(root: ET.Element) -> list[ET.Element]:
    """Flatten testcase elements from testsuites/testsuite JUnit trees."""
    out: list[ET.Element] = []
    for el in root.iter():
        tag = el.tag.split("}")[-1]
        if tag == "testcase":
            out.append(el)
    return out


def merge_junits(staging: Path, suite_name: str, module: str) -> Path | None:
    out_path = merged_junit_path(staging, module)
    inputs = iter_try_junit_files(staging, out_path)
    if not inputs:
        print("No per-attempt JUnit XML (*-try*.xml) to merge.", file=sys.stderr)
        return None

    merged_cases: list[ET.Element] = []
    for path in inputs:
        try:
            tree = ET.parse(path)
        except ET.ParseError as e:
            print(f"Skipping corrupt JUnit {path}: {e}", file=sys.stderr)
            continue
        merged_cases.extend(collect_testcases(tree.getroot()))

    if not merged_cases:
        print("No test cases found in try reports.", file=sys.stderr)
        return None

    total = len(merged_cases)
    failures = errors = skipped = 0
    elapsed = 0.0
    for tc in merged_cases:
        has_failure = has_error = has_skipped = False
        for child in tc:
            tag = child.tag.split("}")[-1]
            if tag == "failure":
                has_failure = True
            elif tag == "error":
                has_error = True
            elif tag == "skipped":
                has_skipped = True
        if has_skipped:
            skipped += 1
        elif has_error:
            errors += 1
        elif has_failure:
            failures += 1
        try:
            elapsed += float(tc.get("time") or 0)
        except ValueError:
            pass

    testsuites = ET.Element(
        "testsuites",
        {
            "tests": str(total),
            "failures": str(failures),
            "errors": str(errors),
            "skipped": str(skipped),
            "time": str(round(elapsed, 4)),
        },
    )
    suite = ET.SubElement(
        testsuites,
        "testsuite",
        {
            "name": suite_name,
            "tests": str(total),
            "failures": str(failures),
            "errors": str(errors),
            "skipped": str(skipped),
            "time": str(round(elapsed, 4)),
        },
    )
    for tc in merged_cases:
        suite.append(tc)

    tree = ET.ElementTree(testsuites)
    ET.indent(tree, space="  ")
    tree.write(out_path, encoding="utf-8", xml_declaration=True)

    for path in inputs:
        try:
            path.unlink()
        except OSError as e:
            print(f"Could not remove {path}: {e}", file=sys.stderr)

    return out_path


def copy_tree_contents(src: Path, dest: Path) -> None:
    dest.mkdir(parents=True, exist_ok=True)
    for child in src.iterdir():
        target = dest / child.name
        if child.is_dir():
            if target.exists():
                shutil.rmtree(target)
            shutil.copytree(child, target)
        else:
            shutil.copy2(child, target)


def bitrise_test_results_dir() -> str | None:
    return os.environ.get("BITRISE_TEST_RESULT_DIR")


def maestro_test_info_name(module: str, tag: str) -> str:
    if module == "financial-connections":
        base = "FC Maestro Tests"
    elif module == "connect":
        base = "Connect Maestro Tests"
    else:
        base = "Maestro Tests"
    return f"{base} - {tag}"


def main() -> None:
    args = parse_args()
    staging: Path = args.staging
    module = args.module
    test_results = Path("/tmp/test_results")

    if not staging.is_dir():
        print(f"Staging directory missing: {staging}", file=sys.stderr)
        sys.exit(0)

    merged = merge_junits(staging, args.tag, module)
    if merged is None:
        sys.exit(0)

    if staging.resolve() != test_results.resolve():
        copy_tree_contents(staging, test_results)

    bitrise_dir_raw = bitrise_test_results_dir()
    if bitrise_dir_raw:
        dest = Path(bitrise_dir_raw) / module
        dest.mkdir(parents=True, exist_ok=True)
        copy_tree_contents(test_results, dest)
        test_name = maestro_test_info_name(module, args.tag)
        info_path = dest / "test-info.json"
        info_path.write_text(json.dumps({"test-name": test_name}, separators=(",", ":")), encoding="utf-8")
        print(f"Published Maestro results under {dest}")
    else:
        print(
            "BITRISE_TEST_RESULT_DIR unset; skipped Bitrise export.",
            file=sys.stderr,
        )


if __name__ == "__main__":
    main()
