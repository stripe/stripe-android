#!/usr/bin/env bash
set -euo pipefail

# Generate a CHANGELOG entry JSON for a given commit using the `llm` CLI.
# Usage: changelog_entry_generator.sh <commit-sha> [pr-number]

COMMIT="${1:?usage: changelog_entry_generator.sh <commit-sha> [pr-number]}"
PR_NUMBER="${2:-}"
REPO="stripe/stripe-android"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROMPT_FILE="$SCRIPT_DIR/changelog_generator_system_prompt.md"
LLM_MODEL="litellm-claude-opus-4.6"
MAX_DIFF_BYTES=150000

if [[ ! -f "$PROMPT_FILE" ]]; then
  echo "error: prompt file not found at $PROMPT_FILE" >&2
  exit 1
fi

# Use a known-good pyenv Python so the `llm` CLI resolves regardless of the
# repo's .python-version.
export PYENV_VERSION=3.11.10

if [[ -z "$PR_NUMBER" ]]; then
  PR_NUMBER="$(git log -1 --format=%s "$COMMIT" | sed -nE 's/.*\(#([0-9]+)\).*/\1/p' || true)"
fi

if [[ -z "$PR_NUMBER" ]] && command -v gh >/dev/null 2>&1; then
  PR_NUMBER="$(GH_HOST=github.com gh api "repos/$REPO/commits/$COMMIT/pulls" --jq '.[0].number // empty' 2>/dev/null || true)"
fi

if [[ -z "$PR_NUMBER" ]]; then
  echo "error: could not determine PR number for commit $COMMIT" >&2
  exit 1
fi

DIFF_FILE="$(mktemp)"
cleanup() { rm -f "$DIFF_FILE"; }
trap cleanup EXIT

if ! git diff "${COMMIT}^" "$COMMIT" > "$DIFF_FILE" 2>/dev/null; then
  git show "$COMMIT" --format= --no-color > "$DIFF_FILE"
fi

DIFF_BYTES="$(wc -c < "$DIFF_FILE" | tr -d ' ')"
if [[ "$DIFF_BYTES" -gt "$MAX_DIFF_BYTES" ]]; then
  head -c "$MAX_DIFF_BYTES" "$DIFF_FILE" > "${DIFF_FILE}.part"
  mv "${DIFF_FILE}.part" "$DIFF_FILE"
  printf '\n\n[diff truncated to %s of %s bytes]\n' "$MAX_DIFF_BYTES" "$DIFF_BYTES" >> "$DIFF_FILE"
fi

jq -n \
  --arg pr_url "https://github.com/$REPO/pull/$PR_NUMBER" \
  --arg pr_number "$PR_NUMBER" \
  --arg hash "$COMMIT" \
  --rawfile diff_text "$DIFF_FILE" \
  '{pr_url: $pr_url, pr_number: $pr_number, hash: $hash, diff_text: $diff_text}' \
  | llm -m "$LLM_MODEL" -s "$(cat "$PROMPT_FILE")"
