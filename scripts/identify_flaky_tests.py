#!/usr/bin/env python3
"""
Identify flaky tests from Bitrise CI build history and output a quarantine list.

Quarantine criteria (7-day rolling window):
  Criterion 1: Test fails >= 2 times on the master branch
  Criterion 2: Test fails >= 10% of the time across master + PR builds
               (requires a minimum of 50 total runs)

Usage:
  python3 identify_flaky_tests.py \
      --app-slug <bitrise-app-slug> \
      --token <bitrise-api-token> \
      [--days 7] \
      [--workflows test,check,run-instrumentation-tests,...] \
      [--output quarantine.json]
"""
from __future__ import annotations

import argparse
import io
import json
import sys
import zipfile
from datetime import datetime, timedelta, timezone
from typing import Any
from urllib.request import Request, urlopen
from urllib.error import URLError
from xml.etree import ElementTree as ET

DEFAULT_WORKFLOWS = [
    "test",
    "check",
    "run-instrumentation-tests",
    "run-paymentsheet-instrumentation-tests",
    "run-cardscan-instrumentation-tests",
    "run-financial-connections-instrumentation-tests",
]

CRITERION_1_MIN_MASTER_FAILURES = 2
CRITERION_2_MIN_FAILURE_RATE = 0.10
CRITERION_2_MIN_RUNS = 50
LOOKBACK_DAYS = 7


def bitrise_get(url: str, token: str) -> Any:
    req = Request(url, headers={"Authorization": f"token {token}"})
    try:
        with urlopen(req, timeout=30) as resp:
            return json.loads(resp.read())
    except URLError as exc:
        print(f"Request failed: {url}: {exc}", file=sys.stderr)
        return {}


def download_bytes(url: str) -> bytes:
    req = Request(url)
    try:
        with urlopen(req, timeout=60) as resp:
            return resp.read()
    except URLError as exc:
        print(f"Download failed: {exc}", file=sys.stderr)
        return b""


def fetch_builds(
    app_slug: str,
    token: str,
    cutoff: datetime,
    workflows: set[str],
) -> list[dict]:
    """Fetch all builds within the time window across master and PR branches."""
    all_builds: list[dict] = []
    next_cursor: str | None = None

    while True:
        url = f"https://api.bitrise.io/v0.1/apps/{app_slug}/builds?limit=50"
        if next_cursor:
            url += f"&next={next_cursor}"

        data = bitrise_get(url, token)
        builds = data.get("data", [])
        if not builds:
            break

        in_window: list[dict] = []
        passed_cutoff = False

        for build in builds:
            triggered = build.get("triggered_at", "")
            if not triggered:
                continue
            dt = datetime.fromisoformat(triggered.replace("Z", "+00:00"))
            if dt >= cutoff:
                if build.get("triggered_workflow") in workflows:
                    in_window.append(build)
            else:
                passed_cutoff = True
                break

        all_builds.extend(in_window)

        paging = data.get("paging", {})
        if passed_cutoff or not paging.get("next"):
            break
        next_cursor = paging["next"]

    return all_builds


def parse_junit_xml(xml_bytes: bytes) -> list[tuple[str, str, bool, bool]]:
    """Parse JUnit XML; return list of (classname, name, failed, skipped)."""
    results: list[tuple[str, str, bool, bool]] = []
    try:
        root = ET.fromstring(xml_bytes)
        if root.tag == "testsuites":
            suites = root.findall("testsuite")
        elif root.tag == "testsuite":
            suites = [root]
        else:
            suites = []

        for suite in suites:
            suite_name = suite.get("name", "")
            for tc in suite.findall("testcase"):
                classname = tc.get("classname", "") or suite_name
                name = tc.get("name", "")
                failed = tc.find("failure") is not None or tc.find("error") is not None
                skipped = tc.find("skipped") is not None
                if classname or name:
                    results.append((classname, name, failed, skipped))
    except ET.ParseError:
        pass
    return results


def extract_test_results(
    app_slug: str,
    token: str,
    build_slug: str,
    min_zip_bytes: int = 500,
) -> dict[str, tuple[int, int]]:
    """
    Download test_results.zip for a build and parse JUnit XML files.
    Returns {test_id: (runs, failures)}.
    """
    data = bitrise_get(
        f"https://api.bitrise.io/v0.1/apps/{app_slug}/builds/{build_slug}/artifacts",
        token,
    )
    artifacts = data.get("data", [])

    zip_art = next(
        (
            a
            for a in artifacts
            if a.get("title") == "test_results.zip"
            and a.get("file_size_bytes", 0) >= min_zip_bytes
        ),
        None,
    )
    if not zip_art:
        return {}

    art_data = bitrise_get(
        f"https://api.bitrise.io/v0.1/apps/{app_slug}/builds/{build_slug}/artifacts/{zip_art['slug']}",
        token,
    )
    dl_url = art_data.get("data", {}).get("expiring_download_url", "")
    if not dl_url:
        return {}

    raw = download_bytes(dl_url)
    if not raw:
        return {}

    test_counts: dict[str, list[int]] = {}  # {test_id: [runs, failures]}
    try:
        with zipfile.ZipFile(io.BytesIO(raw)) as zf:
            xml_names = [n for n in zf.namelist() if n.endswith(".xml") and "TEST-" in n]
            for xml_name in xml_names:
                for classname, name, failed, skipped in parse_junit_xml(zf.read(xml_name)):
                    if skipped:
                        continue
                    test_id = f"{classname}#{name}"
                    if test_id not in test_counts:
                        test_counts[test_id] = [0, 0]
                    test_counts[test_id][0] += 1
                    if failed:
                        test_counts[test_id][1] += 1
    except zipfile.BadZipFile:
        return {}

    return {tid: (counts[0], counts[1]) for tid, counts in test_counts.items()}


def analyze(
    app_slug: str,
    token: str,
    days: int = LOOKBACK_DAYS,
    workflows: list[str] | None = None,
) -> dict[str, Any]:
    """
    Core analysis: fetch builds, extract test data, apply quarantine criteria.
    Returns a dict with quarantine candidates and supporting statistics.
    """
    workflow_set = set(workflows or DEFAULT_WORKFLOWS)
    cutoff = datetime.now(timezone.utc) - timedelta(days=days)

    print(f"Fetching builds from the past {days} days ...", file=sys.stderr)
    builds = fetch_builds(app_slug, token, cutoff, workflow_set)
    print(f"Found {len(builds)} builds in scope.", file=sys.stderr)

    master_builds = [b for b in builds if b.get("branch") == "master"]
    all_builds_by_wf: dict[str, int] = {}
    for b in builds:
        wf = b.get("triggered_workflow", "")
        all_builds_by_wf[wf] = all_builds_by_wf.get(wf, 0) + 1

    # Aggregate per-test counts
    # {test_id: {"master_failures": int, "total_runs": int, "total_failures": int}}
    test_data: dict[str, dict[str, int]] = {}

    processed = 0
    for build in builds:
        slug = build["slug"]
        is_master = build.get("branch") == "master"

        results = extract_test_results(app_slug, token, slug)
        if not results:
            continue

        processed += 1
        if processed % 20 == 0:
            print(f"  Processed {processed} builds ...", file=sys.stderr)

        for test_id, (runs, failures) in results.items():
            if test_id not in test_data:
                test_data[test_id] = {"master_failures": 0, "total_runs": 0, "total_failures": 0}
            test_data[test_id]["total_runs"] += runs
            test_data[test_id]["total_failures"] += failures
            if is_master and failures > 0:
                test_data[test_id]["master_failures"] += failures

    print(f"Processed {processed} builds with test results.", file=sys.stderr)
    print(f"Unique tests observed: {len(test_data)}", file=sys.stderr)

    # Apply quarantine criteria
    quarantine: list[dict[str, Any]] = []

    for test_id, stats in test_data.items():
        master_failures = stats["master_failures"]
        total_runs = stats["total_runs"]
        total_failures = stats["total_failures"]
        failure_rate = total_failures / total_runs if total_runs > 0 else 0.0

        criterion_1_met = master_failures >= CRITERION_1_MIN_MASTER_FAILURES
        criterion_2_met = (
            total_runs >= CRITERION_2_MIN_RUNS
            and failure_rate >= CRITERION_2_MIN_FAILURE_RATE
        )

        if criterion_1_met or criterion_2_met:
            classname, _, testname = test_id.partition("#")
            quarantine.append(
                {
                    "testId": test_id,
                    "className": classname,
                    "testName": testname,
                    "masterFailures": master_failures,
                    "totalRuns": total_runs,
                    "totalFailures": total_failures,
                    "failureRate": round(failure_rate, 4),
                    "criterion1": criterion_1_met,
                    "criterion2": criterion_2_met,
                }
            )

    quarantine.sort(key=lambda x: (-x["totalFailures"], x["testId"]))

    return {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "lookbackDays": days,
        "totalBuildsAnalyzed": len(builds),
        "masterBuildsAnalyzed": len(master_builds),
        "buildsWithTestResults": processed,
        "quarantinedTests": quarantine,
    }


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--app-slug", required=True, help="Bitrise app slug")
    p.add_argument("--token", required=True, help="Bitrise API token")
    p.add_argument("--days", type=int, default=LOOKBACK_DAYS, help="Lookback window in days")
    p.add_argument(
        "--workflows",
        default=",".join(DEFAULT_WORKFLOWS),
        help="Comma-separated list of workflow names to include",
    )
    p.add_argument(
        "--output",
        default="-",
        help="Output file path (default: stdout)",
    )
    return p.parse_args()


def main() -> None:
    args = parse_args()
    workflows = [w.strip() for w in args.workflows.split(",") if w.strip()]

    result = analyze(
        app_slug=args.app_slug,
        token=args.token,
        days=args.days,
        workflows=workflows,
    )

    output_json = json.dumps(result, indent=2)

    if args.output == "-":
        print(output_json)
    else:
        with open(args.output, "w", encoding="utf-8") as fh:
            fh.write(output_json)
        print(f"Results written to {args.output}", file=sys.stderr)

    candidates = result["quarantinedTests"]
    if candidates:
        print(f"\n{'='*60}", file=sys.stderr)
        print(f"Tests to quarantine ({len(candidates)}):", file=sys.stderr)
        for item in candidates:
            reasons = []
            if item["criterion1"]:
                reasons.append(f"master failures={item['masterFailures']}")
            if item["criterion2"]:
                reasons.append(
                    f"failure rate={item['failureRate']:.1%} ({item['totalFailures']}/{item['totalRuns']} runs)"
                )
            print(f"  [{', '.join(reasons)}] {item['testId']}", file=sys.stderr)
    else:
        print("\nNo tests meet the quarantine criteria.", file=sys.stderr)


if __name__ == "__main__":
    main()
