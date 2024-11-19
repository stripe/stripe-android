package com.stripe.android.connect.webview

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.EmbeddedComponentManager.Configuration
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse

@OptIn(PrivateBetaConnectSDK::class)
class StripeConnectWebViewClientTest {

    private val mockContext: Context = mock()
    private val mockSettings: WebSettings = mock {
        on { userAgentString } doReturn "user-agent"
    }
    private val mockWebView: WebView = mock {
        on { settings } doReturn mockSettings
        on { context } doReturn mockContext
    }

    private lateinit var container: StripeConnectWebViewContainerImpl
    private val webViewClient get() = container.stripeWebViewClient

    @Before
    fun setup() {
        val onLoadJsCallback = argumentCaptor<ValueCallback<String>>()
        whenever(mockWebView.evaluateJavascript(any(), onLoadJsCallback.capture())).thenAnswer {
            // Simulate successful JavaScript execution
            onLoadJsCallback.lastValue.onReceiveValue("null")
            null
        }

        val embeddedComponentManager = EmbeddedComponentManager(
            configuration = Configuration(publishableKey = "pk_test_123"),
            fetchClientSecretCallback = { },
        )

        container = StripeConnectWebViewContainerImpl(
            embeddedComponent = StripeEmbeddedComponent.PAYOUTS,
            embeddedComponentManager = embeddedComponentManager,
            logger = Logger.getInstance(enableLogging = false),
            jsonSerializer = Json { ignoreUnknownKeys = true },
        )
    }

    @Test
    fun `configureAndLoadWebView sets user agent`() {
        container.initializeWebView(mockWebView)

        verify(mockWebView).webViewClient = webViewClient
        verify(mockSettings).userAgentString = "user-agent - stripe-android/${StripeSdkVersion.VERSION_NAME}"
    }

    @Test
    fun `onPageStarted initializes javascript bridge`() {
        webViewClient.onPageStarted(mockWebView, "https://example.com", null)

        verify(mockWebView).evaluateJavascript(anyString(), anyOrNull())
    }

    @Test
    fun `configureAndLoadWebView sets publishable key in url`() {
        webViewClient.configureAndLoadWebView(mockWebView)

        verify(mockWebView).loadUrl(
            "https://connect-js.stripe.com/v1.0/android_webview.html#component=payouts&publicKey=pk_test_123"
        )
    }

    @Test
    fun `shouldOverrideUrlLoading doesn't handle request when view or request is null, returns false`() {
        val resultNullView = webViewClient.shouldOverrideUrlLoading(view = null, request = mock())
        assertFalse(resultNullView)

        val resultNullRequest = webViewClient.shouldOverrideUrlLoading(view = mockWebView, request = null)
        assertFalse(resultNullRequest)
    }

    @Test
    fun `shouldOverrideUrlLoading allows request and returns false for allowlisted hosts`() {
        val uri = mockUri("https", "connect-js.stripe.com", "/allowlisted")
        val mockRequest = mock<WebResourceRequest> {
            on { url } doReturn uri
        }

        val result = webViewClient.shouldOverrideUrlLoading(mockWebView, mockRequest)
        assertFalse(result)
    }

    @Test
    fun `shouldOverrideUrlLoading launches ChromeCustomTab for https urls`() {
        val mockUri = mockUri("https", "example.com", "/test")
        val mockRequest = mock<WebResourceRequest> {
            on { url } doReturn mockUri
        }

        val result = webViewClient.shouldOverrideUrlLoading(mockWebView, mockRequest)

        assert(result)
        verify(mockStripeIntentLauncher).launchSecureExternalWebTab(mockContext, mockUri)
    }

    @Test
    fun `shouldOverrideUrlLoading blocks url loading and returns true for unsupported schemes`() {
        val mockUri = mockUri("mailto", "example.com", "/email")
        val mockRequest = mock<WebResourceRequest> {
            on { url } doReturn mockUri
        }

        val result = webViewClient.shouldOverrideUrlLoading(mockWebView, mockRequest)
        assert(result)
    }

    private fun mockUri(scheme: String, host: String, path: String): Uri {
        return mock<Uri> {
            on { this.scheme } doReturn scheme
            on { this.host } doReturn host
            on { this.path } doReturn path
            on { toString() } doReturn "$scheme://$host$path"
        }
    }
}
