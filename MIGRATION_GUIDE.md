# Stripe Android SDK v25 Migration Guide

Automated migration assistance for upgrading from Stripe Android SDK v21+ to v25.

This guide provides step-by-step instructions for using our automated migration tools to upgrade your app from Stripe Android SDK v21+ to v25.

## ⚡ TL;DR - Complete Workflow

```bash
# 1. Download migration tools
curl -L https://github.com/stripe/stripe-android/archive/master.zip -o stripe-android.zip
unzip stripe-android.zip && cd stripe-android-master

# 2. Set up your project (automated)
./setup-migration.sh /path/to/your-android-project

# 3. Follow the printed instructions to update build.gradle files
# 4. Run migration
cd /path/to/your-android-project && ./gradlew migrateToV25

# 5. Apply suggested changes from the HTML report
```

## 🚀 Quick Start

### Step 1: Get Migration Tools

First, download the migration tools from the Stripe Android SDK repository:

#### Option A: Download Migration Tools Only
```bash
# Create a temporary directory
mkdir stripe-migration-temp && cd stripe-migration-temp

# Download the migration tools
curl -L https://github.com/stripe/stripe-android/archive/master.zip -o stripe-android.zip
unzip stripe-android.zip
mv stripe-android-master/migration-rules ./
mv stripe-android-master/config ./
mv stripe-android-master/setup-migration.sh ./
mv stripe-android-master/MIGRATION_GUIDE.md ./

# Clean up
rm -rf stripe-android-master stripe-android.zip
```

#### Option B: Clone the Repository
```bash
git clone https://github.com/stripe/stripe-android.git
cd stripe-android
```

### Step 2: Add Migration Tools to Your Project

Choose one of the following setup methods:

#### Option A: Automated Setup (Recommended)

1. **Run the setup script:**
   ```bash
   # From the stripe-migration-temp directory (if using Option A above)
   ./setup-migration.sh /path/to/your-android-project
   
   # OR from the stripe-android directory (if using Option B above)
   ./setup-migration.sh /path/to/your-android-project
   ```
   
   This will automatically:
   - Copy migration rules to your project
   - Copy detekt configuration
   - Copy this migration guide to your project
   - Validate your project structure

#### Option B: Manual Setup

1. **Copy migration files to your project:**
   ```bash
   # From your download location (stripe-migration-temp or stripe-android directory)
   cp -r migration-rules/ /path/to/your-project/
   mkdir -p /path/to/your-project/config/detekt/
   cp config/detekt/migration.yml /path/to/your-project/config/detekt/
   ```

#### Next Steps (Required for Both Options)

2. **Update your `settings.gradle`:**
   ```gradle
   include ':migration-rules'
   ```

3. **Update your root `build.gradle`:**
   ```gradle
   // Add/ensure detekt plugin is applied
   plugins {
       id 'io.gitlab.arturbosch.detekt' version '1.23.7'
   }
   // OR if using legacy plugin syntax:
   // apply plugin: "io.gitlab.arturbosch.detekt"
   
   dependencies {
       detektPlugins project(':migration-rules')
   }
   
   // Add the migration tasks
   tasks.register('detektMigration', io.gitlab.arturbosch.detekt.Detekt) {
       description = 'Run detekt with v25 migration rules'
       group = 'migration'
       
       buildUponDefaultConfig = false
       parallel = true
       autoCorrect = true
       config.setFrom(files("config/detekt/migration.yml"))
       setSource(files("${projectDir}"))
       include("**/*.kt")
       exclude("**/build/**", "**/migration-rules/**")
       
       reports {
           html.enabled = true
           xml.enabled = true  
           txt.enabled = true
       }
   }
   
   tasks.register('migrateToV25') {
       group = 'migration'
       description = 'Migrates codebase from Stripe Android SDK v21+ to v25'
       dependsOn 'detektMigration'
       
       doLast {
           println "\n🎉 Migration analysis completed!"
           println "📋 Check the detekt reports for migration suggestions:"
           println "   - HTML: build/reports/detekt/detekt.html" 
           println "   - Text: build/reports/detekt/detekt.txt"
           println "📝 Copy-paste the suggested replacements into your code"
           println "🐛 Report issues at: https://github.com/stripe/stripe-android/issues"
       }
   }
   ```

### Step 3: Run Migration Analysis

```bash
./gradlew migrateToV25
```

### Step 4: Apply Migration Changes

1. **Open the HTML report:** `build/reports/detekt/detekt.html`
2. **For each reported issue:**
   - Find the file and line number
   - Copy the "Replace with:" suggestion
   - Paste it into your code

## 📋 What Gets Migrated

### ✅ Automatically Detected & Suggested

| **Before (v21)** | **After (v25)** |
|------------------|-----------------|
| `rememberPaymentSheet(paymentResultCallback = callback)` | `remember(callback) { PaymentSheet.Builder(callback) }.build()` |
| `rememberPaymentSheet(createIntentCallback, paymentResultCallback)` | `remember(createIntent, result) { PaymentSheet.Builder(result).createIntentCallback(createIntent) }.build()` |
| `rememberPaymentSheetFlowController(...)` | `remember(...) { PaymentSheet.FlowController.Builder(...) }.build()` |

### 🔍 Example Migration Output

```
/src/main/java/MainActivity.kt:45:32: Replace with: remember(viewModel::handleResult) { PaymentSheet.Builder(viewModel::handleResult) }.build() [RememberPaymentSheetMigration]
```

**What to do:**
1. Open `MainActivity.kt` line 45
2. Replace the existing `rememberPaymentSheet(...)` with the suggested code
3. Done!

## 🛠 Troubleshooting

### "No issues found"
- ✅ Your code is already v25 compatible!
- Make sure you're using deprecated functions that need migration

### "detekt command not found"
```bash
# Add detekt to your build.gradle if not already present:
plugins {
    id 'io.gitlab.arturbosch.detekt' version '1.23.7'
}
```

### Complex migrations not handled
Some complex cases may need manual review:
- Custom wrapper functions around Stripe APIs
- Dynamically constructed parameters
- The tool will flag these for manual review

## 📚 Manual Migration Steps

For changes not covered by the tool:

### Update your dependencies
```gradle
implementation 'com.stripe:stripe-android:25.+'
```

### Review deprecation warnings
After running the migration tool, check for any remaining deprecation warnings in your IDE.

## ✅ Verification

After migration:

1. **Build your project:** `./gradlew build`
2. **Run tests:** `./gradlew test`
3. **Test key payment flows** in your app
4. **Check that all deprecation warnings are resolved**

## 🐛 Getting Help

- **Script issues:** [Report on GitHub](https://github.com/stripe/stripe-android/issues)
- **Migration questions:** Check our [v25 migration documentation](link-to-docs)
- **General support:** [Stripe Support](https://support.stripe.com/)

## 🎯 Success Checklist

- [ ] Migration tool ran successfully
- [ ] Applied all suggested replacements
- [ ] Project builds without errors  
- [ ] Tests pass
- [ ] No deprecation warnings remain
- [ ] Payment flows work correctly

---

💡 **Pro Tip:** The migration tool handles the complex parameter parsing for you. Even if you need to apply changes manually, the exact replacement code is provided, saving you hours of figuring out the new API patterns!