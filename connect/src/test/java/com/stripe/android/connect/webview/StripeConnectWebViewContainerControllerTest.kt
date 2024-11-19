package com.stripe.android.connect.webview

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.webkit.WebResourceRequest
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.EmbeddedComponentManager.Configuration
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.Colors
import com.stripe.android.core.Logger
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(PrivateBetaConnectSDK::class)
@RunWith(RobolectricTestRunner::class)
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

    private val lifecycleOwner = TestLifecycleOwner()
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
    fun `shouldOverrideUrlLoading allows request and returns false for allowlisted hosts`() {
        val uri = Uri.parse("https://connect-js.stripe.com/allowlisted")
        val mockRequest = mock<WebResourceRequest> {
            on { url } doReturn uri
        }

        val result = controller.shouldOverrideUrlLoading(mockContext, mockRequest)
        assertFalse(result)
    }

    @Test
    fun `shouldOverrideUrlLoading launches ChromeCustomTab for https urls`() {
        val uri = Uri.parse("https://example.com/test")
        val mockRequest = mock<WebResourceRequest> {
            on { url } doReturn uri
        }

        val result = controller.shouldOverrideUrlLoading(mockContext, mockRequest)

        assertTrue(result)
        verify(mockStripeIntentLauncher).launchSecureExternalWebTab(mockContext, uri)
    }

    @Test
    fun `shouldOverrideUrlLoading opens email client for mailto urls`() {
        val uri = Uri.parse("mailto://example@stripe.com")
        val mockRequest = mock<WebResourceRequest> {
            on { url } doReturn uri
        }

        val result = controller.shouldOverrideUrlLoading(mockContext, mockRequest)
        verify(mockStripeIntentLauncher).launchEmailLink(mockContext, uri)
        assertTrue(result)
    }

    @Test
    fun `shouldOverrideUrlLoading opens system launcher for non-http urls`() {
        val uri = Uri.parse("stripe://example@stripe.com")
        val mockRequest = mock<WebResourceRequest> {
            on { url } doReturn uri
        }

        val result = controller.shouldOverrideUrlLoading(mockContext, mockRequest)
        verify(mockStripeIntentLauncher).launchUrlWithSystemHandler(mockContext, uri)
        assertTrue(result)
    }

    fun `should bind to appearance changes`() = runTest {
        assertThat(controller.stateFlow.value.appearance).isNull()

        controller.onCreate(lifecycleOwner)
        val newAppearance = Appearance()
        embeddedComponentManager.update(newAppearance)

        assertThat(controller.stateFlow.value.appearance).isEqualTo(newAppearance)
    }

    @Test
    fun `view should update appearance`() = runTest {
        val appearances = listOf(Appearance(), Appearance(colors = Colors(primary = Color.CYAN)))
        controller.onCreate(lifecycleOwner)

        // Shouldn't update appearance until pageDidLoad is received.
        verify(view, never()).updateConnectInstance(any())

        embeddedComponentManager.update(appearances[0])
        controller.onViewAttached()
        controller.onPageStarted()
        verify(view, never()).updateConnectInstance(any())

        // Should update appearance when pageDidLoad is received.
        controller.onReceivedPageDidLoad()

        // Should update again when appearance changes.
        embeddedComponentManager.update(appearances[1])

        inOrder(view) {
            verify(view).updateConnectInstance(appearances[0])
            verify(view).updateConnectInstance(appearances[1])
        }
    }
}
