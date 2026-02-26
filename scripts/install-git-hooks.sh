#!/bin/bash
#
# Install git hooks for the stripe-android repository

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
HOOKS_DIR="$REPO_ROOT/.git/hooks"

echo "Installing git hooks..."

# Install pre-commit hook
cp "$SCRIPT_DIR/pre-commit" "$HOOKS_DIR/pre-commit"
chmod +x "$HOOKS_DIR/pre-commit"

echo "✓ Pre-commit hook installed successfully!"
echo ""
echo "The hook will run these checks before each commit:"
echo "  - ktlint (code style)"
echo "  - detekt (static analysis)"
echo "  - lintRelease (Android lint)"
echo "  - apiCheck (API compatibility)"
echo "  - verifyReleaseResources (resource verification)"
echo ""
echo "You can skip the hook with: git commit --no-verify"
