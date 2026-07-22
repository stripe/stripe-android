package com.stripe.android

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TEMPORARY DIAGNOSTIC (PR #13565 experiment). Delete before merge.
 *
 * Browser-based LPM tests (e.g. TestAlipay) are silently skipped on CI because
 * [com.stripe.android.test.core.ui.Selectors.getInstalledBrowsers] returns empty. We don't yet know
 * whether that's because the emulator has no browser installed, or because Android 11+
 * package-visibility filtering hides it from getInstalledApplications().
 *
 * The prior diagnostic used android.util.Log (logcat), which Bitrise does not capture. This test
 * instead surfaces the installed-package inventory by failing intentionally: failure messages ARE
 * printed to the Gradle/Bitrise console. It dumps from both the app-under-test context (what the
 * real detection uses) and the test-APK context (which holds QUERY_ALL_PACKAGES) so we can also see
 * whether visibility differs by context.
 */
@RunWith(AndroidJUnit4::class)
internal class TestBrowserInventoryDiagnostic {

    @Test
    fun dumpInstalledBrowserPackages() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        val targetPackages = instrumentation.targetContext.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .map { it.packageName }
            .sorted()
        val testPackages = instrumentation.context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .map { it.packageName }
            .sorted()

        fun browserish(packages: List<String>) = packages.filter {
            it.contains("chrome") || it.contains("firefox") ||
                it.contains("browser") || it.contains("webview")
        }

        throw AssertionError(
            "BROWSER_INVENTORY_DIAGNOSTIC\n" +
                "targetContext: total=${targetPackages.size} browserish=${browserish(targetPackages)}\n" +
                "testContext:   total=${testPackages.size} browserish=${browserish(testPackages)}\n" +
                "targetContext all=$targetPackages"
        )
    }
}
