#!/usr/bin/env python3
"""
Split Gradle managed-device shard XML into per-test-class reports.

AGP writes one XML per shard (e.g. TEST-pixel2api33_0-_paymentsheet-.xml) where the
root <testsuite name="..."> often reflects only the first class in the shard, while
<testcase classname="..."> carries the real owning class. Downstream consumers (and
humans) expect one suite per class — this script regroups testcases by classname,
merges across shards for the same managed-device run, and writes:

  <out-dir>/<gradle-module-path>/<managed-device-run>/TEST-<ClassName>.xml

Use --input-dir for the tree that contains shard XML (often a copied /tmp/test_results
layout); module segments in paths are resolved relative to that root. --out-dir selects
where per-class files are written (default: <input-dir>/managed-device-by-class).

Each output file is a standard JUnit document with a single <testsuite> whose
metadata matches its testcases.
"""

from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path


def discover_managed_device_xml(repo_root: Path) -> list[Path]:
    paths: list[Path] = []
    pattern = "**/build/outputs/androidTest-results/managedDevice/**/*.xml"
    for p in repo_root.glob(pattern):
        if p.is_file():
            paths.append(p)
    return sorted(paths)


def gradle_module_path(repo_root: Path, xml_path: Path) -> str:
    """Return e.g. ':paymentsheet' or ':ml-core:default' from .../<module>/build/outputs/..."""
    try:
        rel = xml_path.resolve().relative_to(repo_root.resolve())
    except ValueError:
        rel = xml_path
    parts = rel.parts
    try:
        bi = parts.index("build")
    except ValueError:
        return ":unknown"
    mod_parts = parts[:bi]
    if not mod_parts:
        return ":unknown"
    return ":" + ":".join(mod_parts)


def managed_device_run_subpath(xml_path: Path) -> str:
    """Path under managedDevice/, e.g. 'debug/pixel2api33'."""
    parts = xml_path.parts
    try:
        idx = parts.index("managedDevice")
    except ValueError:
        return "unknown"
    tail = parts[idx + 1 : -1]
    return "/".join(tail) if tail else "unknown"


def parse_float(s: str | None, default: float = 0.0) -> float:
    if s is None or s == "":
        return default
    try:
        return float(s)
    except ValueError:
        return default


def clone_element(el: ET.Element) -> ET.Element:
    return ET.fromstring(ET.tostring(el, encoding="unicode"))


def testcase_outcomes(tc: ET.Element) -> tuple[bool, bool, bool]:
    has_failure = any(c.tag == "failure" for c in tc)
    has_error = any(c.tag == "error" for c in tc)
    has_skipped = any(c.tag == "skipped" for c in tc)
    return has_failure, has_error, has_skipped


def aggregate_suite_stats(testcases: list[ET.Element]) -> tuple[int, int, int, int, float]:
    tests = len(testcases)
    failures = errors = skipped = 0
    total_time = 0.0
    for tc in testcases:
        f, e, sk = testcase_outcomes(tc)
        failures += int(f)
        errors += int(e)
        skipped += int(sk)
        total_time += parse_float(tc.get("time"))
    return tests, failures, errors, skipped, total_time


def parse_shard_testsuite(path: Path) -> ET.Element:
    tree = ET.parse(path)
    root = tree.getroot()
    if root.tag != "testsuite":
        raise ValueError(f"Expected root <testsuite> in {path}, got <{root.tag}>")
    return root


def collect_properties(suite: ET.Element) -> dict[str, str]:
    props: dict[str, str] = {}
    props_el = suite.find("properties")
    if props_el is None:
        return props
    for p in props_el.findall("property"):
        name = p.get("name")
        value = p.get("value")
        if name is not None and value is not None:
            props[name] = value
    return props


def merge_property_devices(existing: str | None, new_val: str | None) -> str | None:
    if not new_val:
        return existing
    if not existing:
        return new_val
    devices = []
    for part in existing.split(","):
        part = part.strip()
        if part and part not in devices:
            devices.append(part)
    for part in new_val.split(","):
        part = part.strip()
        if part and part not in devices:
            devices.append(part)
    return ",".join(devices)


@dataclass
class RunAccumulator:
    testcases_by_class: dict[str, list[ET.Element]] = field(default_factory=lambda: defaultdict(list))
    props_by_key: dict[str, str] = field(default_factory=dict)
    timestamps: list[str] = field(default_factory=list)
    hostnames: list[str] = field(default_factory=list)


def ingest_shard(run: RunAccumulator, suite: ET.Element) -> None:
    props = collect_properties(suite)
    for k, v in props.items():
        if k == "device":
            run.props_by_key[k] = merge_property_devices(run.props_by_key.get("device"), v) or v
        else:
            if k not in run.props_by_key:
                run.props_by_key[k] = v
    ts = suite.get("timestamp")
    if ts:
        run.timestamps.append(ts)
    hn = suite.get("hostname")
    if hn:
        run.hostnames.append(hn)

    for child in list(suite):
        if child.tag != "testcase":
            continue
        classname = child.get("classname") or ""
        if not classname:
            classname = "(no-classname)"
        run.testcases_by_class[classname].append(clone_element(child))


def build_output_testsuite(
    classname: str,
    testcases: list[ET.Element],
    run: RunAccumulator,
    device_run: str,
) -> ET.Element:
    tests, failures, errors, skipped, total_time = aggregate_suite_stats(testcases)
    suite = ET.Element("testsuite")
    suite.set("name", classname)
    suite.set("tests", str(tests))
    suite.set("failures", str(failures))
    suite.set("errors", str(errors))
    suite.set("skipped", str(skipped))
    suite.set("time", f"{total_time:.3f}")
    if run.timestamps:
        suite.set("timestamp", max(run.timestamps))
    else:
        suite.set("timestamp", "")
    if run.hostnames:
        suite.set("hostname", run.hostnames[0])
    else:
        suite.set("hostname", "localhost")

    props_el = ET.Element("properties")
    merged = dict(run.props_by_key)
    merged.setdefault("managedDeviceRun", device_run)
    for name in sorted(merged.keys()):
        p = ET.Element("property")
        p.set("name", name)
        p.set("value", merged[name])
        props_el.append(p)
    suite.append(props_el)

    for tc in sorted(testcases, key=lambda t: (t.get("name") or "", t.get("classname") or "")):
        suite.append(tc)
    return suite


def classname_to_filename(classname: str) -> str:
    # Match Gradle/JUnit naming (inner classes use '$' in classname).
    return f"TEST-{classname}.xml"


def split_managed_device_results(
    input_dir: Path,
    *,
    output_dir: Path | None = None,
) -> list[Path]:
    """
    Split managed-device shard XML discovered under input_dir.

    Writes under output_dir, or input_dir / 'managed-device-by-class' if omitted.
    """
    input_resolved = input_dir.resolve()
    out_dir = (
        output_dir if output_dir is not None else input_resolved / "managed-device-by-class"
    ).resolve()

    xml_files = discover_managed_device_xml(input_resolved)
    if not xml_files:
        return []

    # (module, device_run) -> RunAccumulator
    buckets: dict[tuple[str, str], RunAccumulator] = defaultdict(RunAccumulator)

    for xf in xml_files:
        mod = gradle_module_path(input_resolved, xf)
        run_path = managed_device_run_subpath(xf)
        bucket = buckets[(mod, run_path)]
        suite = parse_shard_testsuite(xf)
        ingest_shard(bucket, suite)

    written: list[Path] = []
    for (mod, device_run), acc in sorted(buckets.items()):
        mod_segments = [s for s in mod.lstrip(":").split(":") if s]
        dest_dir = out_dir.joinpath(*mod_segments, *device_run.split("/"))
        dest_dir.mkdir(parents=True, exist_ok=True)

        for classname, tcs in sorted(acc.testcases_by_class.items()):
            suite_el = build_output_testsuite(classname, tcs, acc, device_run)
            testsuites = ET.Element("testsuites")
            testsuites.append(suite_el)
            ET.indent(testsuites, space="  ")
            out_path = dest_dir / classname_to_filename(classname)
            ET.ElementTree(testsuites).write(out_path, encoding="UTF-8", xml_declaration=True)
            written.append(out_path)

    return written


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Split managed-device shard JUnit XML into per-class suite files."
    )
    parser.add_argument(
        "directory",
        type=Path,
        nargs="?",
        default=None,
        metavar="DIRECTORY",
        help="Root to scan for managed-device XML (same as --input-dir).",
    )
    parser.add_argument(
        "--input-dir",
        type=Path,
        default=None,
        help="Root that contains **/build/outputs/androidTest-results/managedDevice/**/*.xml.",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=None,
        help="Output directory (default: <input-dir>/managed-device-by-class).",
    )
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="Suppress summary line on stderr.",
    )
    args = parser.parse_args()

    input_dir = args.input_dir or args.directory
    if input_dir is None:
        parser.error("pass DIRECTORY or --input-dir")
    inp = input_dir.resolve()
    out = args.out_dir.resolve() if args.out_dir is not None else None
    written = split_managed_device_results(inp, output_dir=out)
    if not args.quiet:
        effective_out = out if out is not None else inp / "managed-device-by-class"
        if written:
            print(
                f"Managed-device split: {len(written)} class-level XML file(s) -> {effective_out}",
                file=sys.stderr,
            )
        else:
            print("Managed-device split: no managedDevice XML found; nothing written.", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
