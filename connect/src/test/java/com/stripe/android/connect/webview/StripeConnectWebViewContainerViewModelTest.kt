package com.stripe.android.connect.webview

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.webkit.PermissionRequest
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.ComponentEvent
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.analytics.ComponentAnalyticsService
import com.stripe.android.connect.analytics.ConnectAnalyticsEvent
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.Colors
import com.stripe.android.connect.util.Clock
import com.stripe.android.connect.webview.serialization.OpenFinancialConnectionsMessage
import com.stripe.android.connect.webview.serialization.SetOnLoadError
import com.stripe.android.connect.webview.serialization.SetOnLoadError.LoadError
import com.stripe.android.connect.webview.serialization.SetOnLoaderStart
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("TooManyFunctions")
@OptIn(PrivateBetaConnectSDK::class)
@RunWith(RobolectricTestRunner::class)
class StripeConnectWebViewContainerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockContext: Context = mock()
    private val mockActivity: Activity = mock()
    private val view: StripeConnectWebView = mock()
    private val mockPermissionRequest: PermissionRequest = mock()
    private val analyticsService: ComponentAnalyticsService = mock()
    private val androidClock: Clock = mock()
    private val embeddedComponentManager: EmbeddedComponentManager = mock()
    private val embeddedComponent: StripeEmbeddedComponent = StripeEmbeddedComponent.PAYOUTS

    private val appearanceFlow = MutableStateFlow(Appearance())
    private val receivedComponentEvents = mutableListOf<ComponentEvent>()

    private val mockStripeIntentLauncher: StripeIntentLauncher = mock()
    private val mockLogger: Logger = mock()

    private val lifecycleOwner = TestLifecycleOwner()
    private lateinit var viewModel: StripeConnectWebViewContainerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        whenever(embeddedComponentManager.appearanceFlow) doReturn appearanceFlow
        whenever(embeddedComponentManager.getStripeURL(any())) doReturn "https://example.com"
        whenever(androidClock.millis()) doReturn -1L

        viewModel = StripeConnectWebViewContainerViewModel(
            application = RuntimeEnvironment.getApplication(),
            analyticsService = analyticsService,
            clock = androidClock,
            embeddedComponentManager = embeddedComponentManager,
            embeddedComponent = embeddedComponent,
            stripeIntentLauncher = mockStripeIntentLauncher,
            logger = mockLogger,
            createWebView = { _, _, _ -> view }
        )
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load URL when view is attached`() {
        val url = "https://connect-js.stripe.com/v1.0/android_webview.html#component=payouts&publicKey=pk_test_123"
        whenever(embeddedComponentManager.getStripeURL(embeddedComponent)) doReturn url

        viewModel.onViewAttached()
        verify(view).loadUrl(url)
    }

    @Test
    fun `shouldOverrideUrlLoading allows request and returns false for allowlisted hosts, logs unexpected pop-up`() {
        val uri = Uri.parse("https://connect-js.stripe.com/allowlisted")

        viewModel.delegate.onReceivedPageDidLoad("page_view_id")
        val result = viewModel.delegate.shouldOverrideUrlLoading(mockContext, uri)
        assertFalse(result)
        verify(analyticsService).track(
            ConnectAnalyticsEvent.ClientError(
                errorCode = "unexpected_popup",
                errorMessage = "Received pop-up for allow-listed host: https://connect-js.stripe.com/allowlisted",
            )
        )
    }

    @Test
    fun `shouldOverrideUrlLoading launches ChromeCustomTab for https urls`() {
        val uri = Uri.parse("https://example.com/test")
        val result = viewModel.delegate.shouldOverrideUrlLoading(mockContext, uri)

        assertTrue(result)
        verify(mockStripeIntentLauncher).launchSecureExternalWebTab(mockContext, uri)
    }

    @Test
    fun `shouldOverrideUrlLoading opens email client for mailto urls`() {
        val uri = Uri.parse("mailto://example@stripe.com")

        val result = viewModel.delegate.shouldOverrideUrlLoading(mockContext, uri)
        verify(mockStripeIntentLauncher).launchEmailLink(mockContext, uri)
        assertTrue(result)
    }

    @Test
    fun `shouldOverrideUrlLoading opens system launcher for non-http urls`() {
        val uri = Uri.parse("stripe://example@stripe.com")

        val result = viewModel.delegate.shouldOverrideUrlLoading(mockContext, uri)
        verify(mockStripeIntentLauncher).launchUrlWithSystemHandler(mockContext, uri)
        assertTrue(result)
    }

    @Test
    fun `should bind to appearance changes`() = runTest(testDispatcher) {
        assertThat(viewModel.stateFlow.value.appearance).isNull()

        viewModel.onCreate(lifecycleOwner)
        val newAppearance = Appearance()
        appearanceFlow.emit(newAppearance)

        assertThat(viewModel.stateFlow.value.appearance).isEqualTo(newAppearance)
    }

    @Test
    fun `should handle SetOnLoaderStart`() = runTest(testDispatcher) {
        collectComponentEvents()
        val message = SetterFunctionCalledMessage(SetOnLoaderStart(""))
        viewModel.delegate.onPageStarted("https://example.com")
        viewModel.delegate.onReceivedSetterFunctionCalled(message)

        val state = viewModel.stateFlow.value
        assertThat(state.receivedSetOnLoaderStart).isTrue()
        assertThat(state.isNativeLoadingIndicatorVisible).isFalse()
        assertThat(receivedComponentEvents).contains(ComponentEvent.Message(message))
    }

    @Test
    fun `should handle SetOnLoadError`() = runTest(testDispatcher) {
        collectComponentEvents()
        val message = SetterFunctionCalledMessage(SetOnLoadError(LoadError("", null)))
        viewModel.delegate.onReceivedSetterFunctionCalled(message)

        assertThat(receivedComponentEvents).contains(ComponentEvent.Message(message))
    }

    @Test
    fun `should handle other messages`() = runTest(testDispatcher) {
        collectComponentEvents()
        val message = SetterFunctionCalledMessage(
            setter = "foo",
            value = SetterFunctionCalledMessage.UnknownValue(JsonNull)
        )
        viewModel.delegate.onReceivedSetterFunctionCalled(message)

        assertThat(receivedComponentEvents).contains(ComponentEvent.Message(message))
    }

    @Test
    fun `onReceivedError should emit LoadError event`() = runTest(testDispatcher) {
        collectComponentEvents()
        viewModel.delegate.onReceivedError(
            requestUrl = "https://stripe.com",
            httpStatusCode = 404,
            errorMessage = "Not Found",
            isMainPageLoad = true
        )

        assertThat(receivedComponentEvents[0]).isInstanceOf(ComponentEvent.LoadError::class.java)
    }

    @Test
    fun `onReceivedError should not emit events outside of main page load`() = runTest(testDispatcher) {
        collectComponentEvents()

        viewModel.delegate.onReceivedError(
            requestUrl = "https://stripe.com",
            httpStatusCode = 404,
            errorMessage = "Not Found",
            isMainPageLoad = false
        )

        assertThat(receivedComponentEvents).isEmpty()
    }

    @Test
    fun `view should update appearance`() = runTest(testDispatcher) {
        val appearances = listOf(Appearance(), Appearance(colors = Colors(primary = Color.CYAN)))
        viewModel.onCreate(lifecycleOwner)

        // Shouldn't update appearance until pageDidLoad is received.
        verify(view, never()).updateConnectInstance(any())

        appearanceFlow.emit(appearances[0])
        viewModel.onViewAttached()
        viewModel.delegate.onPageStarted("https://example.com")
        verify(view, never()).updateConnectInstance(any())

        // Should update appearance when pageDidLoad is received.
        viewModel.delegate.onReceivedPageDidLoad("page_view_id")

        // Should update again when appearance changes.
        appearanceFlow.emit(appearances[1])

        inOrder(view) {
            verify(view).updateConnectInstance(appearances[0])
            verify(view).updateConnectInstance(appearances[1])
        }
    }

    @Test
    fun `onChooseFile should delegate to manager`() = runTest(testDispatcher) {
        val intent = Intent()
        val expected = arrayOf(Uri.parse("content://path/to/file"))
        var actual: Array<Uri>? = null
        wheneverBlocking { embeddedComponentManager.chooseFile(mockActivity, intent) } doReturn expected

        viewModel.delegate.onChooseFile(
            activity = mockActivity,
            filePathCallback = { actual = it },
            requestIntent = intent
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `onOpenFinancialConnections should delegate to manager`() = runTest(testDispatcher) {
        val message = OpenFinancialConnectionsMessage(
            id = "id",
            clientSecret = "client_secret",
            connectedAccountId = "connected_account_id"
        )
        val expected = FinancialConnectionsSheetResult.Canceled
        wheneverBlocking {
            embeddedComponentManager.presentFinancialConnections(
                activity = mockActivity,
                clientSecret = message.clientSecret,
                connectedAccountId = message.connectedAccountId,
            )
        } doReturn expected

        viewModel.delegate.onOpenFinancialConnections(mockActivity, message)

        verify(view).setCollectMobileFinancialConnectionsResult(
            id = message.id,
            result = expected,
        )
    }

    @Test
    fun `onPermissionRequest denies when no supported permissions requested`() = runTest(testDispatcher) {
        whenever(mockPermissionRequest.resources) doReturn arrayOf("unsupported_permission")

        viewModel.delegate.onPermissionRequest(mockActivity, mockPermissionRequest)

        verify(mockPermissionRequest).deny()
        verify(analyticsService).track(
            ConnectAnalyticsEvent.ClientError(
                errorCode = "unexpected_permissions_request",
                errorMessage = "Unexpected permissions 'unsupported_permission' requested",
            )
        )
    }

    @Test
    fun `onPermissionRequest requests camera permission when not granted`() = runTest(testDispatcher) {
        whenever(mockContext.checkPermission(eq(Manifest.permission.CAMERA), any(), any())) doReturn
            PackageManager.PERMISSION_DENIED

        whenever(mockPermissionRequest.resources) doReturn arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        wheneverBlocking { embeddedComponentManager.requestCameraPermission(any()) } doReturn true

        viewModel.delegate.onPermissionRequest(mockActivity, mockPermissionRequest)

        verify(mockPermissionRequest).grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
    }

    @Test
    fun `onPermissionRequest denies permission when camera permission request is rejected`() = runTest(testDispatcher) {
        whenever(mockContext.checkPermission(eq(Manifest.permission.CAMERA), any(), any())) doReturn
            PackageManager.PERMISSION_DENIED

        whenever(mockPermissionRequest.resources) doReturn arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        wheneverBlocking { embeddedComponentManager.requestCameraPermission(any()) } doReturn false

        viewModel.delegate.onPermissionRequest(mockActivity, mockPermissionRequest)

        verify(mockPermissionRequest).deny()
    }

    @Test
    fun `onMerchantIdChanged updates analytics service`() {
        viewModel.delegate.onMerchantIdChanged("merchant_id")
        verify(analyticsService).merchantId = "merchant_id"
    }

    @Test
    fun `emit component created analytic on init`() {
        verify(analyticsService).track(ConnectAnalyticsEvent.ComponentCreated)
    }

    @Test
    fun `emit component viewed analytic on view attach`() {
        // page view id is null since we haven't received pageDidLoad yet
        viewModel.onViewAttached()
        verify(analyticsService).track(ConnectAnalyticsEvent.ComponentViewed(null))

        // once we receive pageDidLoad, we should emit the page view id for subsequent analytics
        viewModel.delegate.onReceivedPageDidLoad("id123")
        viewModel.onViewAttached()
        verify(analyticsService).track(ConnectAnalyticsEvent.ComponentViewed("id123"))
    }

    @Test
    fun `emit unexpected navigation analytic if non-stripe url page started`() {
        whenever(
            embeddedComponentManager.getStripeURL(embeddedComponent)
        ) doReturn "https://stripe.com/test?foo=bar#mytest"

        viewModel.delegate.onPageStarted("https://example.com/test?foo=bar#mytest")

        // when emitting the analytic, we should strip the query params
        verify(analyticsService).track(
            ConnectAnalyticsEvent.WebErrorUnexpectedNavigation("https://example.com/test")
        )
    }

    @Test
    fun `dont emit unexpected navigation analytic if expected stripe url is used on page started`() {
        whenever(
            embeddedComponentManager.getStripeURL(any())
        ) doReturn "https://stripe.com/test?foo=bar#mytest"

        viewModel.delegate.onPageStarted("https://stripe.com/test")

        // when emitting the analytic, we should strip the query params
        verify(analyticsService, never()).track(
            ConnectAnalyticsEvent.WebErrorUnexpectedNavigation("https://stripe.com/test")
        )
    }

    @Test
    fun `emit web page loaded analytic on page finished`() {
        whenever(androidClock.millis()) doReturn 100L
        viewModel.onViewAttached() // register that the page was attached to capture the start of loading

        whenever(androidClock.millis()) doReturn 200L // difference of 100ms to start of load
        viewModel.delegate.onPageFinished("https://stripe.com")
        verify(analyticsService).track(ConnectAnalyticsEvent.WebPageLoaded(100L))
    }

    @Test
    fun `emit web component loaded analytic when received pageDidLoad`() {
        whenever(androidClock.millis()) doReturn 100L
        viewModel.onViewAttached() // register that the page was attached to capture the start of loading

        whenever(androidClock.millis()) doReturn 200L // difference of 100ms to start of load
        viewModel.delegate.onReceivedPageDidLoad("pageView123")
        verify(analyticsService).track(
            ConnectAnalyticsEvent.WebComponentLoaded(
                pageViewId = "pageView123",
                timeToLoadMs = 100L,
                perceivedTimeToLoadMs = 100L,
            )
        )
    }

    @Test
    fun `emit web page error when main page receives an error`() {
        viewModel.delegate.onReceivedError(
            requestUrl = "https://stripe.com",
            httpStatusCode = 404,
            errorMessage = "Not Found",
            isMainPageLoad = true
        )
        verify(analyticsService).track(
            ConnectAnalyticsEvent.WebErrorPageLoad(
                status = 404,
                error = "Not Found",
                url = "https://stripe.com",
            )
        )
    }

    @Test
    fun `emit deserialization error on error to deserialize web message`() {
        viewModel.delegate.onReceivedPageDidLoad("page_view_id")
        viewModel.delegate.onErrorDeserializingWebMessage(
            webFunctionName = "onSetterFunctionCalled",
            error = IllegalArgumentException("Unable to deserialize"),
        )
        verify(analyticsService).track(
            ConnectAnalyticsEvent.WebErrorDeserializeMessage(
                message = "onSetterFunctionCalled",
                error = "IllegalArgumentException",
                pageViewId = "page_view_id",
            )
        )
    }

    private fun TestScope.collectComponentEvents() {
        backgroundScope.launch {
            viewModel.eventFlow.toCollection(receivedComponentEvents)
        }
    }
}
