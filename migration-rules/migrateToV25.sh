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

# Copy migration rules directory (this entire directory)
echo "📁 Setting up migration tools..."
cp -r "$SCRIPT_DIR" "$PROJECT_PATH/migration-rules"

# Copy documentation
cp "$SCRIPT_DIR/MIGRATION_GUIDE.md" "$PROJECT_PATH/"

# Update the source path in build.gradle to point to the user's project
echo "⚙️  Configuring migration..."
sed -i.bak "s|setSource(files(\"/Users/cttsai/stripe/stripe-android/paymentsheet/src/main/java/com/stripe/android/common/analytics/testfile.kt\"))|setSource(files(\"$PROJECT_PATH\"))|" "$PROJECT_PATH/migration-rules/build.gradle"

# Run the migration
echo "🔍 Running migration (auto-fixing code)..."
cd "$PROJECT_PATH/migration-rules"
./gradlew migrateToV25

echo ""
echo "✅ Migration completed!"
echo "📋 Your code has been automatically updated to v25 APIs"
echo "📋 Check the migration report at:"
echo "   $PROJECT_PATH/migration-rules/build/reports/detekt/detekt.html"
echo ""
echo "📖 See MIGRATION_GUIDE.md for verification steps"