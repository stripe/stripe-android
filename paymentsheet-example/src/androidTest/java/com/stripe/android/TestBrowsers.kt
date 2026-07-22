package com.stripe.android

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.stripe.android.test.core.Browser
import com.stripe.android.test.core.TestParameters
import com.stripe.android.test.core.ui.BrowserUI
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This tests that authorization works with firefox and chrome browsers.  If a browser
 * is specified in the test parameters and it is not available the test will be skipped.
 * Note that if a webview is used because there is no browser or a returnURL is specified
 * this it cannot be actuated with UI Automator.
 */
@RunWith(AndroidJUnit4::class)
internal class TestBrowsers : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "bancontact",
    ).copy(
        useBrowser = Browser.Chrome,
    )

    /**
     * Guards the browser device: browser-redirect tests Assume-skip when no browser is detected, so
     * a missing/invisible browser would silently drop their coverage. This fails loudly instead.
     */
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

    @Test
    fun testAuthorizeChrome() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                useBrowser = Browser.Chrome,
            )
        )
    }

    @Ignore("On Google Pixel, the connection to stripe.com is deemed insecure and the page does not load.")
    fun testAuthorizeFirefox() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                useBrowser = Browser.Firefox,
            )
        )
    }

    @Test
    fun testAuthorizeAnyAvailableBrowser() {
        // Does not work when default browser is android
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                useBrowser = null,
            )
        )
    }
}
