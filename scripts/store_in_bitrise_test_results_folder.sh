#!/usr/bin/env bash
# For each module under the staging root (default repo root via BITRISE_SOURCE_DIR),
# copies test output from the path for the given test type into $BITRISE_TEST_RESULT_DIR/<test-name>/
# and writes test-info.json there. If BITRISE_DEPLOY_DIR is set, copies the full test result tree there too.
#
# Usage: store_in_bitrise_test_results_folder.sh {unit|instrumentation} [test_title]
#
# test-name = path segments before /build (slashes → hyphens).
# test-info.json uses "test_title - test_name" when test_title is provided.
set -euo pipefail

usage() {
  echo "Usage: $0 {unit|instrumentation} [test_title]" >&2
  exit 1
}

[[ "${1:-}" ]] || usage
test_title="${2:-}"
case "$1" in
  unit) find_regex='.*/build/test-results/testDebugUnitTest$' ;;
  instrumentation) find_regex='.*/build/instrumentation-test-results$' ;;
  *) usage ;;
esac

# On Bitrise, scan the repo (Gradle outputs) so this can run before copy_test_results_to_tmp.
ROOT="${TEST_RESULTS_ROOT:-${BITRISE_SOURCE_DIR}}"
[[ -d "$ROOT" ]] || { echo "Not a directory: $ROOT" >&2; exit 1; }

echo "Scanning: $ROOT ($1)"
echo "Publishing to: $BITRISE_TEST_RESULT_DIR"

n=0
while IFS= read -r -d '' match_dir; do
  rel="${match_dir#"$ROOT"/}"
  prefix="${rel%%/build/*}"
  test_name="${prefix//\//-}"
  dest_dir="$BITRISE_TEST_RESULT_DIR/$test_name"

  mkdir -p "$dest_dir"

  if [[ -n "$(find "$match_dir" -mindepth 1 -print -quit 2>/dev/null)" ]]; then
    cp -R "$match_dir"/. "$dest_dir/"
  fi

  if [[ -n "$test_title" ]]; then
    json_test_name="$test_title - $test_name"
  else
    json_test_name="$test_name"
  fi

  printf '{"test-name": "%s"}' "$json_test_name" >"$dest_dir/test-info.json"

  echo "  → $dest_dir (from $match_dir)"
  n=$((n + 1))
done < <(find "$ROOT" -type d -regex "$find_regex" -print0)

if [[ "$n" -eq 0 ]]; then
  echo "No matching folders for '$1'." >&2
fi

if [[ -n "${BITRISE_DEPLOY_DIR:-}" ]]; then
  mkdir -p "$BITRISE_DEPLOY_DIR"
  [[ -d "$BITRISE_TEST_RESULT_DIR" ]] || { echo "Not a directory: $BITRISE_TEST_RESULT_DIR" >&2; exit 1; }
  cp -R "$BITRISE_TEST_RESULT_DIR"/. "$BITRISE_DEPLOY_DIR"/
  echo "Copied $BITRISE_TEST_RESULT_DIR → $BITRISE_DEPLOY_DIR"
fi
