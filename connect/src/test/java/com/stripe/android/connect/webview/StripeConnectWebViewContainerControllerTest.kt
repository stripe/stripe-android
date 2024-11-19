package com.stripe.android.connect.webview

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.EmbeddedComponentManager.Configuration
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.core.Logger
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertFalse

@OptIn(PrivateBetaConnectSDK::class)
class StripeConnectWebViewContainerControllerTest {
    private val mockContext: Context = mock()
    private val view: StripeConnectWebViewContainerInternal = mock()
    private val embeddedComponentManager = EmbeddedComponentManager(
        configuration = Configuration(publishableKey = "pk_test_123"),
        fetchClientSecretCallback = { },
    )
    private val embeddedComponent: StripeEmbeddedComponent = StripeEmbeddedComponent.PAYOUTS
    private val mockStripeIntentLauncher: StripeIntentLauncher = mock()
    private val mockLogger: Logger = mock()

    private lateinit var controller: StripeConnectWebViewContainerController

    @Before
    fun setup() {
        controller = StripeConnectWebViewContainerController(
            view = view,
            embeddedComponentManager = embeddedComponentManager,
            embeddedComponent = embeddedComponent,
            stripeIntentLauncher = mockStripeIntentLauncher,
            logger = mockLogger,
        )
    }

    @Test
    fun `should load URL when view is attached`() {
        controller.onViewAttached()
        verify(view).loadUrl(
            "https://connect-js.stripe.com/v1.0/android_webview.html#component=payouts&publicKey=pk_test_123"
        )
    }

    @Test
    fun `shouldOverrideUrlLoading doesn't handle request when request is null, returns false`() {
        val resultNullRequest = controller.shouldOverrideUrlLoading(context = mockContext, request = null)
        assertFalse(resultNullRequest)
    }

    @Test
    fun `shouldOverrideUrlLoading allows request and returns false for allowlisted hosts`() {
        val uri = mockUri("https", "connect-js.stripe.com", "/allowlisted")
        val mockRequest = mock<WebResourceRequest> {
            on { url } doReturn uri
        }

        val result = controller.shouldOverrideUrlLoading(mockContext, mockRequest)
        assertFalse(result)
    }

    @Test
    fun `shouldOverrideUrlLoading launches ChromeCustomTab for https urls`() {
        val mockUri = mockUri("https", "example.com", "/test")
        val mockRequest = mock<WebResourceRequest> {
            on { url } doReturn mockUri
        }

        val result = controller.shouldOverrideUrlLoading(mockContext, mockRequest)

        assert(result)
        verify(mockStripeIntentLauncher).launchSecureExternalWebTab(mockContext, mockUri)
    }

    @Test
    fun `shouldOverrideUrlLoading blocks url loading and returns true for unsupported schemes`() {
        val mockUri = mockUri("mailto", "example.com", "/email")
        val mockRequest = mock<WebResourceRequest> {
            on { url } doReturn mockUri
        }

        val result = controller.shouldOverrideUrlLoading(mockContext, mockRequest)
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
