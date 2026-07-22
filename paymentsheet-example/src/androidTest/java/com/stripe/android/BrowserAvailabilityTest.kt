package com.stripe.android

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.stripe.android.test.core.ui.BrowserUI
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the browser device: browser-redirect tests Assume-skip when no browser is detected, so a
 * missing/invisible browser would silently drop their coverage. This fails loudly instead.
 *
 * Intentionally does NOT extend BasePlaygroundTest: it only needs to check the device inventory, and
 * staying off that harness keeps it clear of leak detection and the playground setup.
 */
@RunWith(AndroidJUnit4::class)
internal class BrowserAvailabilityTest {

    @Test
    fun browserIsInstalledOnDevice() {
        val knownBrowserPackages = BrowserUI.values().map { it.packageName }
        val installedBrowsers = InstrumentationRegistry.getInstrumentation().targetContext
            .packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .map { it.packageName }
            .filter { it in knownBrowserPackages }
        assertThat(installedBrowsers).isNotEmpty()
    }
}
