package com.stripe.android.connect.webview

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StripeConnectWebViewTest {
    private val testUrl = "http://stripe.com"
    private val mockDelegate: StripeConnectWebView.Delegate = mock()

    private lateinit var webView: StripeConnectWebView

    @Before
    fun setup() {
        webView = StripeConnectWebView(
            applicationContext = RuntimeEnvironment.getApplication(),
            delegate = mockDelegate,
            logger = Logger.getInstance(enableLogging = false)
        )
    }

    @Test
    fun `clients are set`() {
        assertThat(webView.webViewClient).isEqualTo(webView.stripeWebViewClient)
        assertThat(webView.webChromeClient).isEqualTo(webView.stripeWebChromeClient)
    }

    @Test
    fun `user agent is set`() {
        assertThat(webView.settings.userAgentString).endsWith(
            " - stripe-android/${StripeSdkVersion.VERSION_NAME}"
        )
    }

    @Test
    fun `WebViewClient onPageStarted is handled`() {
        webView.stripeWebViewClient.onPageStarted(webView, testUrl, null)
        verify(mockDelegate).onPageStarted(testUrl)
    }
}
