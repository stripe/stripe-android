package com.stripe.android.connect.webview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.ComponentListenerDelegate
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PayoutsListener
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.Colors
import com.stripe.android.connect.webview.serialization.SetOnLoadError
import com.stripe.android.connect.webview.serialization.SetOnLoadError.LoadError
import com.stripe.android.connect.webview.serialization.SetOnLoaderStart
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(PrivateBetaConnectSDK::class)
@RunWith(RobolectricTestRunner::class)
class StripeConnectWebViewContainerControllerTest {

    private val mockContext: Context = mock()
    private val mockPermissionRequest: PermissionRequest = mock()
    private val view: StripeConnectWebViewContainerInternal = mock()
    private val embeddedComponentManager: EmbeddedComponentManager = mock()
    private val embeddedComponent: StripeEmbeddedComponent = StripeEmbeddedComponent.PAYOUTS

    private val appearanceFlow = MutableStateFlow(Appearance())
    private val delegateReceivedEvents = mutableListOf<SetterFunctionCalledMessage>()
    private val listener: PayoutsListener = mock()
    private val listenerDelegate: ComponentListenerDelegate<PayoutsListener> =
        ComponentListenerDelegate { delegateReceivedEvents.add(it) }

    private val mockStripeIntentLauncher: StripeIntentLauncher = mock()
    private val mockLogger: Logger = mock()

    private val lifecycleOwner = TestLifecycleOwner()
    private lateinit var controller: StripeConnectWebViewContainerController<PayoutsListener>

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)

        whenever(embeddedComponentManager.appearanceFlow) doReturn appearanceFlow

        controller = StripeConnectWebViewContainerController(
            view = view,
            embeddedComponentManager = embeddedComponentManager,
            embeddedComponent = embeddedComponent,
            listener = listener,
            listenerDelegate = listenerDelegate,
            stripeIntentLauncher = mockStripeIntentLauncher,
            logger = mockLogger,
        )
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load URL when view is attached`() {
        whenever(embeddedComponentManager.getStripeURL(embeddedComponent)) doReturn
            "https://connect-js.stripe.com/v1.0/android_webview.html#component=payouts&publicKey=pk_test_123"

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

    @Test
    fun `should bind to appearance changes`() = runTest {
        assertThat(controller.stateFlow.value.appearance).isNull()

        controller.onCreate(lifecycleOwner)
        val newAppearance = Appearance()
        appearanceFlow.emit(newAppearance)

        assertThat(controller.stateFlow.value.appearance).isEqualTo(newAppearance)
    }

    @Test
    fun `should handle SetOnLoaderStart`() = runTest {
        val message = SetterFunctionCalledMessage(SetOnLoaderStart(""))
        controller.onPageStarted()
        controller.onReceivedSetterFunctionCalled(message)

        val state = controller.stateFlow.value
        assertThat(state.receivedSetOnLoaderStart).isTrue()
        assertThat(state.isNativeLoadingIndicatorVisible).isFalse()
        verify(listener).onLoaderStart()
    }

    @Test
    fun `should handle SetOnLoadError`() = runTest {
        val message = SetterFunctionCalledMessage(SetOnLoadError(LoadError("", null)))
        controller.onReceivedSetterFunctionCalled(message)

        verify(listener).onLoadError(any())
    }

    @Test
    fun `should handle other messages`() = runTest {
        val message = SetterFunctionCalledMessage(
            setter = "foo",
            value = SetterFunctionCalledMessage.UnknownValue(JsonNull)
        )
        controller.onReceivedSetterFunctionCalled(message)

        assertThat(delegateReceivedEvents).contains(message)
    }

    @Test
    fun `onReceivedError should forward to listener`() = runTest {
        controller.onReceivedError("https://stripe.com", 404, "Not Found")

        verify(listener).onLoadError(any())
    }

    @Test
    fun `view should update appearance`() = runTest {
        val appearances = listOf(Appearance(), Appearance(colors = Colors(primary = Color.CYAN)))
        controller.onCreate(lifecycleOwner)

        // Shouldn't update appearance until pageDidLoad is received.
        verify(view, never()).updateConnectInstance(any())

        appearanceFlow.emit(appearances[0])
        controller.onViewAttached()
        controller.onPageStarted()
        verify(view, never()).updateConnectInstance(any())

        // Should update appearance when pageDidLoad is received.
        controller.onReceivedPageDidLoad()

        // Should update again when appearance changes.
        appearanceFlow.emit(appearances[1])

        inOrder(view) {
            verify(view).updateConnectInstance(appearances[0])
            verify(view).updateConnectInstance(appearances[1])
        }
    }

    @Test
    fun `onPermissionRequest grants permission when camera permission is already granted`() = runTest {
        whenever(mockContext.checkPermission(eq(Manifest.permission.CAMERA), any(), any())) doReturn
            PackageManager.PERMISSION_GRANTED

        whenever(mockPermissionRequest.resources).doReturn(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))

        controller.onPermissionRequest(mockContext, mockPermissionRequest)

        verify(mockPermissionRequest).grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
    }

    @Test
    fun `onPermissionRequest denies permission when no supported permissions are requested`() = runTest {
        whenever(mockPermissionRequest.resources) doReturn arrayOf("unsupported_permission")

        controller.onPermissionRequest(mockContext, mockPermissionRequest)

        verify(mockPermissionRequest).deny()
    }

    @Test
    fun `onPermissionRequest requests camera permission when not granted`() = runTest {
        whenever(mockContext.checkPermission(eq(Manifest.permission.CAMERA), any(), any())) doReturn
            PackageManager.PERMISSION_DENIED

        whenever(mockPermissionRequest.resources) doReturn arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        wheneverBlocking { embeddedComponentManager.requestCameraPermission(any()) } doReturn true

        controller.onPermissionRequest(mockContext, mockPermissionRequest)

        verify(mockPermissionRequest).grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
    }

    @Test
    fun `onPermissionRequest denies permission when camera permission request is rejected`() = runTest {
        whenever(mockContext.checkPermission(eq(Manifest.permission.CAMERA), any(), any())) doReturn
            PackageManager.PERMISSION_DENIED

        whenever(mockPermissionRequest.resources) doReturn arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        wheneverBlocking { embeddedComponentManager.requestCameraPermission(any()) } doReturn false

        controller.onPermissionRequest(mockContext, mockPermissionRequest)

        verify(mockPermissionRequest).deny()
    }
}
