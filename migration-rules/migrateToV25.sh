#!/bin/bash

# Stripe Android SDK v25 Migration Script
# This script runs the automated migration analysis for your Android project

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <path-to-your-android-project>"
    echo "Example: $0 /path/to/MyApp"
    exit 1
fi

PROJECT_PATH="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Starting Stripe v25 Migration..."
echo "Project: $PROJECT_PATH"

# Validate project path
if [ ! -f "$PROJECT_PATH/build.gradle" ]; then
    echo "❌ Error: $PROJECT_PATH doesn't appear to be an Android project (no build.gradle found)"
    exit 1
fi

# Run the migration from original location
echo "🔍 Running migration (auto-fixing code)..."
cd "$SCRIPT_DIR"
./gradlew detektMigration -PprojectPath="$PROJECT_PATH"

echo ""
echo "✅ Migration completed!"
echo "📋 Your code has been automatically updated to v25 APIs"

# Only show report if there were issues that couldn't be auto-corrected
if [ -s "$SCRIPT_DIR/build/reports/detekt/detekt.txt" ]; then
    echo ""
    echo "⚠️  Some issues need manual review:"
    echo "   📄 Report: $SCRIPT_DIR/build/reports/detekt/detekt.html"
fi

echo ""
echo "📖 See MIGRATION_GUIDE.md for next steps"