# Stripe Android SDK v25 Migration Guide

Automated migration tool for upgrading from Stripe Android SDK v21+ to v25.

## ⚡ TL;DR - Complete Workflow

```bash
# 1. Download migration tools
curl -L https://github.com/stripe/stripe-android/archive/master.zip -o stripe-android.zip
unzip stripe-android.zip && cd stripe-android-master/migration-rules

# 2. Run migration (automatically fixes your code!)
./migrateToV25.sh /path/to/your-android-project

# 3. Build and test your project
```

## 🚀 Getting Started

### Step 1: Download Migration Tools

```bash
# Download and extract migration tools
curl -L https://github.com/stripe/stripe-android/archive/master.zip -o stripe-android.zip
unzip stripe-android.zip
cd stripe-android-master/migration-rules
```

### Step 2: Run Migration

```bash
# Run the migration script (replace with your project path)
./migrateToV25.sh /path/to/your-android-project
```

**What this does:**
- ✅ Copies migration tools to your project
- ✅ Configures migration for your codebase
- ✅ **Automatically fixes your code** to use v25 APIs
- ✅ Generates detailed reports

### Step 3: Verify Migration

1. **Build your project:** `./gradlew build`
2. **Run tests:** `./gradlew test`
3. **Review the report:** `your-project/migration-rules/build/reports/detekt/detekt.html`
4. **Test key payment flows** in your app

## 📋 What Gets Migrated

### ✅ Automatically Fixed

| **Before (v21)** | **After (v25)** |
|------------------|-----------------|
| `rememberPaymentSheet(paymentResultCallback = callback)` | `remember(callback) { PaymentSheet.Builder(callback) }.build()` |
| `rememberPaymentSheet(createIntentCallback, paymentResultCallback)` | `remember(createIntent, result) { PaymentSheet.Builder(result).createIntentCallback(createIntent) }.build()` |
| `rememberPaymentSheetFlowController(...)` | `remember(...) { PaymentSheet.FlowController.Builder(...) }.build()` |

### 🔍 Example Migration Output

```
✅ Fixed: /src/main/java/MainActivity.kt:45:32
   Before: rememberPaymentSheet(paymentResultCallback = viewModel::handleResult)
   After:  remember(viewModel::handleResult) { PaymentSheet.Builder(viewModel::handleResult) }.build()
```

**Your code is automatically updated** - no manual changes needed!

## 🛠 Troubleshooting

### "No issues found"
- ✅ Your code is already v25 compatible!
- Make sure you're using deprecated functions that need migration

### Script fails or gradle errors
- Ensure your project has a `build.gradle` file in the root
- Check that Java 11+ is available
- Make sure your project builds before running migration

### Some cases need manual review
Complex cases that require manual attention:
- Custom wrapper functions around Stripe APIs
- Dynamically constructed parameters
- The tool will flag these in the report for manual review

## 📚 Manual Migration Steps

For changes not covered by the tool:

### Update your dependencies
```gradle
implementation 'com.stripe:stripe-android:25.+'
```

### Review flagged items
Check the HTML report for any items marked as "needs manual review" and update them according to the v25 migration guide.

## 🐛 Getting Help

- **Script issues:** [Report on GitHub](https://github.com/stripe/stripe-android/issues)
- **General support:** [Stripe Support](https://support.stripe.com/)

---

💡 **Pro Tip:** The migration tool automatically fixes your code with the correct v25 API patterns. Just run it and your code is ready to go!