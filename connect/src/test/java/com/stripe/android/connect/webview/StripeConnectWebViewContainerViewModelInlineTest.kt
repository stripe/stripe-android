package com.stripe.android.connect.webview

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.analytics.ComponentAnalyticsService
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.manager.EmbeddedComponentCoordinator
import com.stripe.android.connect.util.Clock
import com.stripe.android.core.Logger
import com.stripe.android.testing.ViewModelStoreTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StripeConnectWebViewContainerViewModelInlineTest {

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    private val webView: StripeConnectWebView = mock()
    private val analyticsService: ComponentAnalyticsService = mock()
    private val androidClock: Clock = mock()
    private val componentCoordinator: EmbeddedComponentCoordinator = mock()
    private val embeddedComponentManager: EmbeddedComponentManager = mock {
        on { this.coordinator } doReturn componentCoordinator
    }
    private val mockLogger: Logger = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(componentCoordinator.appearanceFlow) doReturn MutableStateFlow(Appearance.default())
        whenever(componentCoordinator.getStripeURL(any())) doReturn "https://example.com"
        whenever(androidClock.millis()) doReturn -1L
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `other components are unaffected`() {
        assertNotInline(StripeEmbeddedComponent.PAYOUTS)
        assertNotInline(StripeEmbeddedComponent.PAYMENTS)
        assertNotInline(StripeEmbeddedComponent.ACCOUNT_ONBOARDING)
    }

    @Test
    fun `sheets are torn down when the component navigates`() {
        val sheetWebView: StripeConnectSheetWebView = mock()
        val viewModel = createBannerViewModelWithSheet(sheetWebView)
        viewModel.delegate.onSheetChanged(SHEET_ID, 612f, isDismissible = true)

        viewModel.delegate.onPageStarted("https://connect-js.stripe.com/v1.0/android_webview.html")

        assertThat(viewModel.sheetsFlow.value).isEmpty()
        verify(sheetWebView).stopLoading()
        verify(webView, never()).sheetClosed(any())
    }

    private fun assertNotInline(component: StripeEmbeddedComponent) {
        var isInline: Boolean? = null
        val viewModel = StripeConnectWebViewContainerViewModel(
            application = RuntimeEnvironment.getApplication(),
            clock = androidClock,
            embeddedComponentManager = embeddedComponentManager,
            embeddedComponent = component,
            analyticsService = analyticsService,
            logger = mockLogger,
            createWebView = { _, _, _, _, inline ->
                isInline = inline
                webView
            },
        ).also { viewModelStoreRule.track(it) }

        assertThat(isInline).isFalse()
        assertThat(viewModel.delegate.onCreateWindow(isUserGesture = true)).isNull()
    }

    private fun createBannerViewModelWithSheet(
        sheetWebView: StripeConnectSheetWebView,
    ): StripeConnectWebViewContainerViewModel {
        whenever(componentCoordinator.getStripeURL(StripeEmbeddedComponent.NOTIFICATION_BANNER))
            .doReturn("https://connect-js.stripe.com/v1.0/android_webview.html#component=notification-banner")
        lateinit var sheetListener: StripeConnectSheetWebView.Listener
        val viewModel = StripeConnectWebViewContainerViewModel(
            application = RuntimeEnvironment.getApplication(),
            clock = androidClock,
            embeddedComponentManager = embeddedComponentManager,
            embeddedComponent = StripeEmbeddedComponent.NOTIFICATION_BANNER,
            analyticsService = analyticsService,
            logger = mockLogger,
            createWebView = { _, _, _, _, _ -> webView },
            createSheetWebView = { listener ->
                sheetListener = listener
                sheetWebView
            },
        ).also { viewModelStoreRule.track(it) }
        viewModel.delegate.onCreateWindow(isUserGesture = false)
        val sheetUrl = Uri.parse(
            "https://connect-js.stripe.com/accessory_layer_abc123.html#os_sheet=true&os_sheet_id=$SHEET_ID"
        )
        assertThat(sheetListener.onWindowNavigated(sheetWebView, sheetUrl)).isTrue()
        return viewModel
    }

    private companion object {
        const val SHEET_ID = "sheet-1"
    }
}
