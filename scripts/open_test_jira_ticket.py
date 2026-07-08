#!/usr/bin/env python3
"""Open a Jira ticket for one or more tests, routed to the owning team.

Given a list of test class names (for example a set of tests that are being
quarantined), this script:

  1. Locates each test's source file in the repository.
  2. Determines the owning team by matching the file path against
     ``.github/CODEOWNERS``.
  3. Maps the owning team to a Jira project.
  4. Files a Jira ticket via the failure-notification endpoint used elsewhere
     in this repository (see ``scripts/notify_failure_endpoint.rb``).

The endpoint URL and HMAC signing key are provided through arguments or
environment variables so that no service address is hard-coded here. Requests
are signed with HMAC-SHA256 and sent in the ``X-TM-Signature`` header, matching
the existing notification script.

Example:

    scripts/open_test_jira_ticket.py \\
        --reason "Quarantined due to flakiness" \\
        PaymentSheetLoadParallelismTest

Use ``--dry-run`` to print the ticket payload(s) without contacting the
endpoint. This is handy for local testing without credentials.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import json
import os
import re
import sys
import urllib.request
from dataclasses import dataclass, field

# Environment variables that carry the ticket-creation endpoint and its signing
# key. These mirror the names used by the existing notification script and are
# supplied as CI secrets rather than committed anywhere.
ENDPOINT_ENV = "SDK_FAILURE_NOTIFICATION_ENDPOINT"
HMAC_KEY_ENV = "SDK_FAILURE_NOTIFICATION_ENDPOINT_HMAC_KEY"

# CODEOWNERS lists broad reviewer groups on every rule. When determining the
# team that actually owns a file, skip these catch-all groups so we resolve to
# the most specific owning team.
GENERIC_OWNER_TEAMS = {
    "stripe/android-sdk-reviewers",
    "stripe/android-sdk-non-core-reviewers",
}

# Jira project used when an owning team has no explicit override below. Test
# reliability / quarantine work for the mobile SDK is tracked in this project.
DEFAULT_JIRA_PROJECT = "RUN_MOBILESDK"

# Per-team routing overrides. Add an entry here to send a team's tickets to a
# different Jira project. Keys are CODEOWNERS team handles (without the leading
# "@"). Teams not listed fall back to DEFAULT_JIRA_PROJECT, and the owning team
# is always recorded on the ticket as a label so it can be triaged correctly.
TEAM_TO_JIRA_PROJECT: dict[str, str] = {}

# Jira component. "Android" is the component used by the existing notification
# script, so we reuse it to stay within known-valid values.
JIRA_COMPONENTS = ["Android"]

# Directories that never contain source we care about; pruning them keeps the
# file search fast in a large repository.
PRUNE_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    "build",
    "node_modules",
    "out",
}

TEST_FILE_EXTENSIONS = (".kt", ".java")


@dataclass
class TicketPlan:
    test_name: str
    file_path: str | None
    team: str | None
    project: str
    summary: str
    description: str
    labels: list[str] = field(default_factory=list)


def repo_root() -> str:
    """Return the repository root (the parent of this script's directory)."""
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def find_test_file(test_name: str, root: str) -> str | None:
    """Return the repo-relative path of the file defining ``test_name``.

    Matches a file whose basename is ``<test_name>.kt`` or ``<test_name>.java``.
    If several match, the first found is returned.
    """
    targets = {f"{test_name}{ext}" for ext in TEST_FILE_EXTENSIONS}
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in PRUNE_DIRS]
        for filename in filenames:
            if filename in targets:
                return os.path.relpath(os.path.join(dirpath, filename), root)
    return None


def parse_codeowners(path: str) -> list[tuple[str, list[str]]]:
    """Parse a CODEOWNERS file into an ordered list of (pattern, owners)."""
    rules: list[tuple[str, list[str]]] = []
    if not os.path.exists(path):
        return rules
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                continue
            parts = stripped.split()
            pattern = parts[0]
            owners = [owner.lstrip("@") for owner in parts[1:]]
            rules.append((pattern, owners))
    return rules


def _pattern_matches(pattern: str, rel_path: str) -> bool:
    """Return True if a CODEOWNERS pattern matches a repo-relative path."""
    if pattern == "*":
        return True
    normalized = pattern.lstrip("/")
    if normalized.endswith("/"):
        # Directory pattern: match anything under that directory.
        return rel_path == normalized[:-1] or rel_path.startswith(normalized)
    # Fall back to glob-style matching for other patterns.
    regex = re.escape(normalized).replace(r"\*", "[^/]*")
    return re.fullmatch(regex, rel_path) is not None


def owners_for_path(rel_path: str, rules: list[tuple[str, list[str]]]) -> list[str]:
    """Return the owners for a path. The last matching rule wins (CODEOWNERS)."""
    matched: list[str] = []
    for pattern, owners in rules:
        if _pattern_matches(pattern, rel_path):
            matched = owners
    return matched


def owning_team(owners: list[str]) -> str | None:
    """Return the most specific owning team, ignoring catch-all reviewer groups."""
    for owner in owners:
        if owner not in GENERIC_OWNER_TEAMS:
            return owner
    return owners[0] if owners else None


def jira_project_for_team(team: str | None) -> str:
    if team is None:
        return DEFAULT_JIRA_PROJECT
    return TEAM_TO_JIRA_PROJECT.get(team, DEFAULT_JIRA_PROJECT)


def team_label(team: str | None) -> str | None:
    """Build a Jira-safe label recording the owning team (labels cannot contain spaces)."""
    if team is None:
        return None
    return "owner-" + re.sub(r"[^A-Za-z0-9._-]", "-", team)


def module_for_path(rel_path: str | None) -> str | None:
    if not rel_path:
        return None
    return rel_path.split("/", 1)[0]


def build_ticket_plan(test_name: str, root: str, rules, reason: str, run_url: str | None) -> TicketPlan:
    rel_path = find_test_file(test_name, root)
    owners = owners_for_path(rel_path, rules) if rel_path else []
    team = owning_team(owners)
    project = jira_project_for_team(team)
    module = module_for_path(rel_path)

    summary = f"{test_name}: {reason}"

    description_lines = [
        f"Test: {test_name}",
        f"Reason: {reason}",
        f"Source file: {rel_path if rel_path else 'not found in repository'}",
        f"Module: {module if module else 'unknown'}",
        f"Owning team: {('@' + team) if team else 'unknown (defaulted)'}",
    ]
    if run_url:
        description_lines.append(f"Related run: {run_url}")
    description_lines.append(
        "Please ACK this ticket and investigate. Team ownership was derived "
        "from .github/CODEOWNERS."
    )
    description = "\n".join(description_lines)

    labels = ["quarantine"]
    label = team_label(team)
    if label:
        labels.append(label)

    return TicketPlan(
        test_name=test_name,
        file_path=rel_path,
        team=team,
        project=project,
        summary=summary,
        description=description,
        labels=labels,
    )


def ticket_payload(plan: TicketPlan) -> dict:
    return {
        "project": plan.project,
        "summary": plan.summary,
        "description": plan.description,
        "components": JIRA_COMPONENTS,
        "labels": plan.labels,
    }


def post_ticket(endpoint: str, hmac_key_b64: str, payload: dict) -> int:
    """Sign and POST a ticket payload. Returns the HTTP status code."""
    body = json.dumps(payload).encode("utf-8")
    key = base64.b64decode(hmac_key_b64)
    signature = base64.b64encode(hmac.new(key, body, hashlib.sha256).digest()).decode("ascii")

    request = urllib.request.Request(
        endpoint,
        data=body,
        headers={
            "Content-Type": "application/json",
            "X-TM-Signature": signature,
        },
        method="POST",
    )
    with urllib.request.urlopen(request) as response:  # noqa: S310 (trusted endpoint from env)
        return response.status


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Open a Jira ticket for tests, routed to the owning team.",
    )
    parser.add_argument("tests", nargs="+", help="Test class names, e.g. PaymentSheetLoadParallelismTest")
    parser.add_argument(
        "--reason",
        default="Quarantined due to flakiness",
        help="Reason recorded on the ticket (default: %(default)s).",
    )
    parser.add_argument(
        "--run-url",
        default=None,
        help="Optional link to a failing CI run to include in the ticket.",
    )
    parser.add_argument(
        "--endpoint",
        default=os.environ.get(ENDPOINT_ENV),
        help=f"Ticket-creation endpoint (default: ${ENDPOINT_ENV}).",
    )
    parser.add_argument(
        "--hmac-key",
        default=os.environ.get(HMAC_KEY_ENV),
        help=f"Base64 HMAC signing key (default: ${HMAC_KEY_ENV}).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the ticket payloads without contacting the endpoint.",
    )
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    root = repo_root()
    rules = parse_codeowners(os.path.join(root, ".github", "CODEOWNERS"))

    plans = [
        build_ticket_plan(test_name, root, rules, args.reason, args.run_url)
        for test_name in args.tests
    ]

    if not args.dry_run and (not args.endpoint or not args.hmac_key):
        print(
            f"Missing endpoint/HMAC key. Provide --endpoint/--hmac-key or set "
            f"${ENDPOINT_ENV} and ${HMAC_KEY_ENV}, or pass --dry-run.",
            file=sys.stderr,
        )
        return 2

    exit_code = 0
    for plan in plans:
        payload = ticket_payload(plan)
        if plan.file_path is None:
            print(f"WARNING: could not find a source file for '{plan.test_name}'.", file=sys.stderr)

        if args.dry_run:
            print(f"[dry-run] Would open ticket for {plan.test_name}:")
            print(json.dumps(payload, indent=2))
            continue

        try:
            status = post_ticket(args.endpoint, args.hmac_key, payload)
            print(f"Opened ticket for {plan.test_name} in {plan.project} (HTTP {status}).")
        except Exception as error:  # noqa: BLE001 - surface any failure per test
            print(f"ERROR: failed to open ticket for {plan.test_name}: {error}", file=sys.stderr)
            exit_code = 1

    return exit_code


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
