#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "test_name",
        help="Maestro YAML basename without .yaml",
    )
    args = parser.parse_args()

    json_path = os.environ.get("BITRISE_QUARANTINED_TESTS_JSON", "").strip()
    if not json_path:
        sys.exit(1)

    path = Path(json_path)
    if not path.is_file():
        print(
            f"Warning: BITRISE_QUARANTINED_TESTS_JSON set but file not found: {json_path}",
            file=sys.stderr,
        )
        sys.exit(1)

    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        sys.exit(1)

    if not isinstance(data, list):
        sys.exit(1)

    for item in data:
        if isinstance(item, dict) and item.get("testCaseName") == args.test_name:
            sys.exit(0)

    sys.exit(1)


if __name__ == "__main__":
    main()
