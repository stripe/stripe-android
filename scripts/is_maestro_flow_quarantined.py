#!/usr/bin/env python3
"""Exit 0 if test_name matches testCaseName in BITRISE_QUARANTINED_TESTS_JSON (JSON body); else exit 1."""

from __future__ import annotations

import argparse
import json
import os
import sys


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "test_name",
        help="Maestro YAML basename without .yaml",
    )
    args = parser.parse_args()

    raw = os.environ.get("BITRISE_QUARANTINED_TESTS_JSON", "").strip()

    if not raw:
        sys.exit(1)

    try:
        data = json.loads(raw)
    except json.JSONDecodeError as e:
        print(
            f"Invalid JSON in BITRISE_QUARANTINED_TESTS_JSON: {e}",
            file=sys.stderr,
        )
        sys.exit(1)

    if not isinstance(data, list):
        sys.exit(1)

    for item in data:
        if isinstance(item, dict) and item.get("testCaseName") == args.test_name:
            sys.exit(0)

    sys.exit(1)


if __name__ == "__main__":
    main()
