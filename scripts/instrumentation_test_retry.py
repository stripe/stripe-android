#!/usr/bin/env python3
"""Track successful instrumentation cases across outer Gradle retries.

Excluding successes, instead of including only reported failures, leaves tests
that never ran (for example after an emulator crash) eligible for the next run.
Only reports written by this invocation are trusted. Earlier successes are kept
for the final Bitrise report even when AGP replaces its output on the next run.
"""

import hashlib
import json
import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def result_directories(root):
    for directory, children, _ in os.walk(root):
        children[:] = [name for name in children if not name.startswith(".")]
        if "build" in children:
            children.remove("build")
            results = Path(directory) / "build/outputs/androidTest-results"
            if results.is_dir():
                yield Path(directory), results


def reports(root):
    for project, directory in result_directories(root):
        for kind in ("connected", "managedDevice"):
            for report in (directory / kind).rglob("TEST-*.xml"):
                yield str(project), report


def fingerprint(path):
    return [path.stat().st_mtime_ns, hashlib.sha256(path.read_bytes()).hexdigest()]


def read_cases(path):
    try:
        return list(ET.parse(path).getroot().iter("testcase"))
    except (ET.ParseError, OSError) as error:
        print(f"Cannot use retry results from {path}: {error}", file=sys.stderr)
        return []


def selector(case):
    class_name, name = case.get("classname", ""), case.get("name", "")
    # AndroidJUnitRunner uses comma-separated Class#method selectors. Keep exact
    # parameterized names; removing their suffix would also exclude failed cases.
    if not class_name or not name or any(c in class_name + name for c in ",#\n\r"):
        return None
    return f"{class_name}#{name}"


def snapshot(state, root, command):
    if not state:
        state.update(
            scope=hashlib.sha256(json.dumps(command).encode()).hexdigest()[:16],
            completed={},
            seen_reports={},
        )
        # Replace only this command's archived results, retaining results from
        # other commands in the same workflow (such as the separate IME task).
        for _, directory in result_directories(root):
            (directory / "retries" / f"TEST-{state['scope']}.xml").unlink(missing_ok=True)
    state["before"] = {str(path): fingerprint(path) for _, path in reports(root)}


def update(state, root):
    observed = {}
    for project, path in reports(root):
        if state["before"].get(str(path)) == fingerprint(path):
            continue
        state["seen_reports"][str(path)] = project
        for case in read_cases(path):
            key = selector(case)
            if key is None:
                continue
            passed = not any(case.find(tag) is not None for tag in ("failure", "error", "skipped"))
            passed = passed and case.get("status", "run") != "notrun"
            previous = observed.get((project, key))
            # The same selector can appear on multiple devices. A failure must
            # win over a success, regardless of report traversal order.
            if previous is None or not passed:
                observed[project, key] = ET.tostring(case, encoding="unicode") if passed else ""
    for (project, key), xml in observed.items():
        completed = state["completed"].setdefault(project, {})
        if xml:
            completed[key] = xml
        else:
            completed.pop(key, None)


def finish(state):
    current = {}
    for filename, project in state["seen_reports"].items():
        path = Path(filename)
        if path.is_file():
            current.setdefault(project, set()).update(selector(case) for case in read_cases(path))
    for project, completed in state["completed"].items():
        missing = [
            ET.fromstring(xml) for key, xml in completed.items()
            if key not in current.get(project, set())
        ]
        if not missing:
            continue
        suite = ET.Element(
            "testsuite", name="Previously passed tests", tests=str(len(missing)),
            failures="0", errors="0", skipped="0",
        )
        suite.extend(missing)
        archive = Path(project) / "build/outputs/androidTest-results/retries" / f"TEST-{state['scope']}.xml"
        archive.parent.mkdir(parents=True, exist_ok=True)
        ET.ElementTree(suite).write(archive, encoding="utf-8", xml_declaration=True)


def main():
    action, filename, *command = sys.argv[1:]
    path = Path(filename)
    state = json.loads(path.read_text() or "{}")
    if action == "snapshot":
        snapshot(state, Path.cwd().resolve(), command)
    elif action == "update":
        update(state, Path.cwd().resolve())
    elif action == "finish":
        finish(state)
    else:
        raise ValueError(f"Unknown action: {action}")
    path.write_text(json.dumps(state))


if __name__ == "__main__":
    main()
