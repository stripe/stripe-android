package com.stripe.hcaptcha.webview

import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.stripe.hcaptcha.HCaptchaStateListener
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaInternalConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class HCaptchaHeadlessWebViewTest {

    @Test
    fun `reset after load destroys webview and ignores subsequent calls`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val headlessWebView = createHeadlessWebView(activity)

        headlessWebView.onLoaded()
        headlessWebView.reset()

        headlessWebView.reset()
        headlessWebView.startVerification(activity)
    }

    @Test
    fun `reset before load destroys webview immediately`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val headlessWebView = createHeadlessWebView(activity)

        headlessWebView.reset()
        headlessWebView.onLoaded()

        headlessWebView.startVerification(activity)
    }

    private fun createHeadlessWebView(activity: FragmentActivity): HCaptchaHeadlessWebView {
        return HCaptchaHeadlessWebView(
            activity = activity,
            config = HCaptchaConfig(siteKey = "test_site_key"),
            internalConfig = HCaptchaInternalConfig(),
            listener = HCaptchaStateListener(
                onOpen = {},
                onSuccess = {},
                onFailure = {},
            ),
        )
    }
}
