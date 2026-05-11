#!/usr/bin/env python3
"""Merge Maestro JUnit try-reports, attach screen recordings, publish for Bitrise Test Reports."""
from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

DEFAULT_STAGING = Path("/tmp/maestro_tests")
MERGED_NAME = "maestro_merged.xml"
VIDEO_EXTENSIONS = {".mp4", ".webm", ".ogg"}


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--tag", required=True, help="JUnit testsuite name (e.g. Maestro tag)")
    p.add_argument("--module", required=True, help="Module label for paths and test-info.json")
    p.add_argument("--staging", type=Path, default=DEFAULT_STAGING, help="Directory containing *_try*.xml")
    p.add_argument(
        "--video-scan",
        type=Path,
        nargs="*",
        default=[Path("/tmp/test_results"), DEFAULT_STAGING],
        help="Extra directories to search for recording files",
    )
    return p.parse_args()


def iter_try_junit_files(staging: Path) -> list[Path]:
    files = sorted(staging.glob("*_try*.xml"))
    merged = staging / MERGED_NAME
    return [p for p in files if p.resolve() != merged.resolve()]


def collect_testcases(root: ET.Element) -> list[ET.Element]:
    """Flatten testcase elements from testsuites/testsuite JUnit trees."""
    out: list[ET.Element] = []
    for el in root.iter():
        tag = el.tag.split("}")[-1]
        if tag == "testcase":
            out.append(el)
    return out


def testcase_identity(tc: ET.Element) -> str:
    return tc.get("id") or tc.get("name") or tc.get("classname") or "unknown"


def normalized_lower_id(raw: str) -> str:
    return raw.lower().replace("_", "-")


def video_prefixes(module: str, testcase_id: str) -> list[str]:
    lid = normalized_lower_id(testcase_id)
    prefixes = [lid]
    if module == "connect":
        prefixes.append(f"connect-{lid}")
    prefixes.sort(key=len, reverse=True)
    return prefixes


def list_video_paths(*dirs: Path) -> list[Path]:
    found: list[Path] = []
    for d in dirs:
        if not d.is_dir():
            continue
        try:
            for p in d.iterdir():
                if p.is_file() and p.suffix.lower() in VIDEO_EXTENSIONS:
                    found.append(p)
        except OSError:
            continue
    return found


def pick_video_for_testcase(module: str, testcase_id: str, dirs: list[Path]) -> Path | None:
    prefs = video_prefixes(module, testcase_id)
    candidates: list[Path] = []
    for p in list_video_paths(*dirs):
        name_lower = p.name.lower()
        for pref in prefs:
            if name_lower.startswith(pref):
                candidates.append(p)
                break
    if not candidates:
        return None
    candidates.sort(key=lambda x: x.stat().st_mtime, reverse=True)
    return candidates[0]


def ensure_attachment_property(testcase: ET.Element, relative_name: str) -> None:
    props = None
    for child in testcase:
        local = child.tag.split("}")[-1]
        if local == "properties":
            props = child
            break
    if props is None:
        props = ET.SubElement(testcase, "properties")
    ET.SubElement(props, "property", {"name": "attachment_video", "value": relative_name})


def merge_junits(staging: Path, suite_name: str, module: str, video_scan: list[Path]) -> Path | None:
    inputs = iter_try_junit_files(staging)
    if not inputs:
        print("No *_try*.xml files to merge.", file=sys.stderr)
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

    scan_dirs = [staging, *video_scan]
    used_names: dict[str, Path] = {}

    for tc in merged_cases:
        tid = testcase_identity(tc)
        src = pick_video_for_testcase(module, tid, scan_dirs)
        if src is None:
            continue
        dest_name = src.name
        dest = staging / dest_name
        if dest.resolve() != src.resolve():
            if dest.exists():
                dest.unlink()
            shutil.copy2(src, dest)
        ensure_attachment_property(tc, dest_name)

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

    out_path = staging / MERGED_NAME
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


def main() -> None:
    args = parse_args()
    staging: Path = args.staging
    module = args.module

    if not staging.is_dir():
        print(f"Staging directory missing: {staging}", file=sys.stderr)
        sys.exit(0)

    merged = merge_junits(staging, args.tag, module, list(args.video_scan))
    if merged is None:
        sys.exit(0)

    test_results = Path("/tmp/test_results")
    copy_tree_contents(staging, test_results)

    bitrise_dir = os.environ.get("BITRISE_TEST_RESULT_DIR")
    if bitrise_dir:
        dest = Path(bitrise_dir) / module
        copy_tree_contents(staging, dest)
        info_path = dest / "test-info.json"
        test_name = f"{module} - {args.tag}"
        info_path.write_text(json.dumps({"test-name": test_name}, separators=(",", ":")), encoding="utf-8")
        print(f"Published Maestro results under {dest}")
    else:
        print("BITRISE_TEST_RESULT_DIR unset; skipped Bitrise export.", file=sys.stderr)


if __name__ == "__main__":
    main()
