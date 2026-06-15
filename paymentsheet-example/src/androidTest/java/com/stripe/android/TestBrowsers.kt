package com.stripe.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.test.core.Browser
import com.stripe.android.test.core.TestParameters
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

    @Test
    fun testAuthorizeChrome() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                useBrowser = Browser.Chrome,
            )
        )
    }

    @Ignore("On browserstack's Google Pixel, the connection to stripe.com is deemed insecure and the page does not load.")
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
