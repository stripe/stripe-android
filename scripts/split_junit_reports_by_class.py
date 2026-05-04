#!/usr/bin/env python3
"""
Split merged or batch JUnit XML reports into one file per test class.

Reads all XML files matching a glob pattern, groups <testcase> elements by
the classname attribute (falling back to the parent <testsuite name=...> when
missing), and writes TEST-<ClassName>.xml files with aggregated stats.

Usage:
  split_junit_reports_by_class.py <glob_pattern> <output_directory>

Example:
  split_junit_reports_by_class.py "build/outputs/**/test-results/**/*.xml" /tmp/by-class
"""

from __future__ import annotations

import argparse
import glob
import os
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from xml.dom import minidom


def _local_name(tag: str) -> str:
    if "}" in tag:
        return tag.rsplit("}", 1)[-1]
    return tag


def _float_time(raw: str | None) -> float:
    if raw is None or raw == "":
        return 0.0
    try:
        return float(raw)
    except ValueError:
        return 0.0


def _iter_testsuites(root: ET.Element) -> list[ET.Element]:
    if _local_name(root.tag) == "testsuites":
        return [c for c in root if _local_name(c.tag) == "testsuite"]
    if _local_name(root.tag) == "testsuite":
        return [root]
    return []


def _clone(el: ET.Element) -> ET.Element:
    return ET.fromstring(ET.tostring(el, encoding="unicode"))


def _classify_testcase(testcase: ET.Element) -> str:
    """Returns 'failed' | 'error' | 'skipped' | 'passed'."""
    for child in testcase:
        ln = _local_name(child.tag)
        if ln == "failure":
            return "failed"
        if ln == "error":
            return "error"
        if ln in ("skipped", "ignored"):
            return "skipped"
    return "passed"


def _count_outcomes(
    testcases: list[ET.Element],
) -> tuple[int, int, int, int, float]:
    """Returns (tests, failures, errors, skipped, total_time)."""
    tests = len(testcases)
    failures = 0
    errors = 0
    skipped = 0
    total_time = 0.0
    for tc in testcases:
        total_time += _float_time(tc.get("time"))
        outcome = _classify_testcase(tc)
        if outcome == "failed":
            failures += 1
        elif outcome == "error":
            errors += 1
        elif outcome == "skipped":
            skipped += 1
    return tests, failures, errors, skipped, total_time


def _merge_properties(
    target: ET.Element, sources: list[ET.Element]
) -> None:
    seen: set[tuple[str, str]] = set()
    for src in sources:
        for ch in src:
            if _local_name(ch.tag) != "property":
                continue
            name = ch.get("name", "")
            value = ch.get("value", "")
            key = (name, value)
            if key in seen:
                continue
            seen.add(key)
            target.append(_clone(ch))


def _prettify_xml(root: ET.Element) -> str:
    rough = ET.tostring(root, encoding="unicode")
    parsed = minidom.parseString(rough)
    return parsed.toprettyxml(indent="  ")


def _write_testsuite(
    classname: str,
    testcases: list[ET.Element],
    property_sources: list[ET.Element],
    out_path: str,
) -> None:
    tests, failures, errors, skipped, total_time = _count_outcomes(testcases)
    ts = ET.Element("testsuite")
    ts.set("name", classname)
    ts.set("tests", str(tests))
    ts.set("failures", str(failures))
    ts.set("errors", str(errors))
    ts.set("skipped", str(skipped))
    ts.set("time", f"{total_time:.3f}")
    if property_sources:
        props = ET.SubElement(ts, "properties")
        _merge_properties(props, property_sources)
    for tc in testcases:
        ts.append(_clone(tc))
    raw = _prettify_xml(ts)
    lines = [ln for ln in raw.splitlines() if ln.strip()]
    header = '<?xml version="1.0" encoding="UTF-8"?>'
    body = "\n".join(lines[1:]) if lines[0].startswith("<?xml") else "\n".join(lines)
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(header + "\n")
        f.write(body + "\n")


def _safe_filename(classname: str) -> str:
    # Gradle-style: TEST-com.example.FooTest.xml
    safe = classname.replace(os.sep, ".")
    return f"TEST-{safe}.xml"


def split_reports(glob_pattern: str, output_dir: str) -> int:
    paths = sorted(
        {os.path.realpath(p) for p in glob.glob(glob_pattern, recursive=True) if os.path.isfile(p)}
    )
    if not paths:
        print(f"No files matched pattern: {glob_pattern!r}", file=sys.stderr)
        return 1

    # classname -> list of testcase elements (cloned)
    by_class: dict[str, list[ET.Element]] = defaultdict(list)
    # classname -> list of <properties> parents to pull property children from
    property_parents: dict[str, list[ET.Element]] = defaultdict(list)

    for path in paths:
        try:
            tree = ET.parse(path)
        except ET.ParseError as e:
            print(f"Skip (invalid XML): {path}: {e}", file=sys.stderr)
            continue
        root = tree.getroot()
        suites = _iter_testsuites(root)
        if not suites:
            print(f"Skip (no testsuite/testsuites): {path}", file=sys.stderr)
            continue

        for suite in suites:
            suite_name = suite.get("name", "")
            props_container = None
            for ch in suite:
                if _local_name(ch.tag) == "properties":
                    props_container = ch
                    break

            suite_by_class: dict[str, list[ET.Element]] = defaultdict(list)
            for child in suite:
                if _local_name(child.tag) != "testcase":
                    continue
                classname = child.get("classname") or suite_name
                if not classname:
                    classname = "unknown.UnknownClass"
                suite_by_class[classname].append(_clone(child))

            for classname, cases in suite_by_class.items():
                by_class[classname].extend(cases)
                if props_container is not None:
                    property_parents[classname].append(props_container)

    if not by_class:
        print("No test cases found in matched files.", file=sys.stderr)
        return 1

    os.makedirs(output_dir, exist_ok=True)

    for classname, cases in sorted(by_class.items()):
        out_name = _safe_filename(classname)
        out_path = os.path.join(output_dir, out_name)
        prop_srcs = property_parents.get(classname, [])
        _write_testsuite(classname, cases, prop_srcs, out_path)
        print(out_path)

    return 0


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(
        description="Split JUnit XML reports into one file per test class."
    )
    p.add_argument(
        "glob_pattern",
        help='Glob for input XML files (use quotes; enable ** with recursive=True internally)',
    )
    p.add_argument(
        "output_directory",
        help="Directory for TEST-<ClassName>.xml outputs",
    )
    args = p.parse_args(argv)
    return split_reports(args.glob_pattern, args.output_directory)


if __name__ == "__main__":
    sys.exit(main())
