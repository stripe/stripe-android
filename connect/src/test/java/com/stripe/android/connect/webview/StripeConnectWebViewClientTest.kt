package com.stripe.android.connect.webview

import android.webkit.WebSettings
import android.webkit.WebView
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever

@OptIn(PrivateBetaConnectSDK::class)
class StripeConnectWebViewClientTest {

    private val mockWebView: WebView = mock()
    private val mockSettings: WebSettings = mock()

    private lateinit var webViewClient: StripeConnectWebViewClient

    @Before
    fun setup() {
        webViewClient = StripeConnectWebViewClient(
            StripeConnectURL.Component.PAYOUTS,
            Logger.getInstance(enableLogging = false),
            Json { ignoreUnknownKeys = true },
        )
        whenever(mockWebView.settings).thenReturn(mockSettings)
    }

    @Test
    fun `onPageStarted should initialize JavaScript bridge`() {
        webViewClient.onPageStarted(mockWebView, "https://example.com", null)
        verify(mockWebView).evaluateJavascript(anyString(), isNull())
    }

    @Test
    fun `configureAndLoadWebView sets user agent`() {
        whenever(mockSettings.userAgentString).thenReturn("user-agent")

        webViewClient.configureAndLoadWebView(mockWebView)

        verify(mockWebView).webViewClient = webViewClient
        verify(mockSettings).userAgentString = "user-agent - stripe-android/${StripeSdkVersion.VERSION_NAME}"
    }

    @Test
    fun `configureAndLoadWebView sets publishable key in url`() {
        EmbeddedComponentManager.init(EmbeddedComponentManager.Configuration("pk_test_123")) {}

        webViewClient.configureAndLoadWebView(mockWebView)

        verify(mockWebView).loadUrl("https://connect-js.stripe.com/v1.0/android_webview.html#component=payouts&publicKey=pk_test_123")
    }
}
