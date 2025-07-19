#!/bin/bash

# Stripe Android SDK v25 Migration Setup Script
# This script sets up the migration tools in your Android project

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <path-to-your-android-project>"
    echo "Example: $0 /path/to/MyApp"
    exit 1
fi

PROJECT_PATH="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Setting up Stripe v25 Migration Tools..."
echo "Project: $PROJECT_PATH"

# Validate project path
if [ ! -f "$PROJECT_PATH/build.gradle" ]; then
    echo "❌ Error: $PROJECT_PATH doesn't appear to be an Android project (no build.gradle found)"
    exit 1
fi

# Copy migration rules
echo "📁 Copying migration rules..."
cp -r "$SCRIPT_DIR/migration-rules" "$PROJECT_PATH/"

# Create config directory if it doesn't exist
mkdir -p "$PROJECT_PATH/config/detekt"
cp "$SCRIPT_DIR/config/detekt/migration.yml" "$PROJECT_PATH/config/detekt/"

# Copy documentation
cp "$SCRIPT_DIR/MIGRATION_GUIDE.md" "$PROJECT_PATH/"

echo "✅ Migration tools installed!"
echo ""
echo "📋 Next steps:"
echo "1. Follow the setup instructions in MIGRATION_GUIDE.md"
echo "2. Add migration configuration to your build files"
echo "3. Run './gradlew migrateToV25'"
echo ""
echo "📖 See MIGRATION_GUIDE.md for complete setup instructions"