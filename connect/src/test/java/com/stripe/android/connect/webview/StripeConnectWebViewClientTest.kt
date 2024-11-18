package com.stripe.android.connect.webview

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.content.Context
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
import org.mockito.kotlin.any
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
    private val mockStripeIntentLauncher: StripeIntentLauncher = mock()

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
            connectComponent = StripeEmbeddedComponent.PAYOUTS,
            stripeIntentLauncher = mockStripeIntentLauncher,
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
}
