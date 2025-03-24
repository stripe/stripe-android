package com.stripe.android.connect.webview

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.webview.serialization.AccountSessionClaimedMessage
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.ConnectJson
import com.stripe.android.connect.webview.serialization.CustomFontSourceJs
import com.stripe.android.connect.webview.serialization.OpenAuthenticatedWebViewMessage
import com.stripe.android.connect.webview.serialization.OpenFinancialConnectionsMessage
import com.stripe.android.connect.webview.serialization.PageLoadMessage
import com.stripe.android.connect.webview.serialization.SetOnExit
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(PrivateBetaConnectSDK::class)
@RunWith(RobolectricTestRunner::class)
class StripeConnectWebViewTest {
    private val testUrl = "http://stripe.com"
    private val mockDelegate: StripeConnectWebView.Delegate = mock()

    private lateinit var webView: StripeConnectWebView

    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val containerView = FrameLayout(activity)

    @Before
    fun setup() {
        activity.setContentView(containerView)
        webView = StripeConnectWebView(
            application = RuntimeEnvironment.getApplication(),
            delegate = mockDelegate,
            logger = Logger.getInstance(enableLogging = true)
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

    @Test
    fun `WebViewClient onPageFinished is handled`() {
        webView.stripeWebViewClient.onPageFinished(webView, testUrl)
        verify(mockDelegate).onPageFinished(testUrl)
    }

    @Test
    fun `WebChromeClient onPermissionRequest is handled`() {
        containerView.addView(webView)
        webView.stripeWebChromeClient.onPermissionRequest(mock())
        verifyBlocking(mockDelegate) { onPermissionRequest(eq(activity), any()) }
    }

    @Test
    fun `WebChromeClient onShowFileChooser is handled`() {
        val intent = Intent()
        val filePathCallback: ValueCallback<Array<Uri>> = mock()
        val fileChooserParams: WebChromeClient.FileChooserParams = mock {
            on { createIntent() } doReturn intent
        }

        containerView.addView(webView)
        webView.stripeWebChromeClient.onShowFileChooser(
            webView = webView,
            filePathCallback = filePathCallback,
            fileChooserParams = fileChooserParams
        )

        verifyBlocking(mockDelegate) { onChooseFile(activity, filePathCallback, intent) }
    }

    @Test
    fun `JS fetchInitComponentProps is handled`() {
        val props = JsonObject(mapOf("foo" to JsonPrimitive("bar")))
        whenever(mockDelegate.propsJson).doReturn(props)
        assertThat(webView.stripeJsInterface.fetchInitComponentProps())
            .isEqualTo(ConnectJson.encodeToString(props))
    }

    @Test
    fun `JS fetchInitParams is handled`() {
        val params = ConnectInstanceJs(fonts = listOf(CustomFontSourceJs("foo", "bar")))
        whenever(mockDelegate.getInitialParams(any())).doReturn(params)
        assertThat(webView.stripeJsInterface.fetchInitParams())
            .isEqualTo(ConnectJson.encodeToString(params))
    }

    @Test
    fun `JS onSetterFunctionCalled is handled`() {
        val message = SetterFunctionCalledMessage(SetOnExit)
        webView.stripeJsInterface.onSetterFunctionCalled(ConnectJson.encodeToString(message))
        verify(mockDelegate).onReceivedSetterFunctionCalled(message)
    }

    @Test
    fun `JS openAuthenticatedWebView is handled`() {
        val message = OpenAuthenticatedWebViewMessage("test-id", "https://test.stripe.com")
        containerView.addView(webView)
        webView.stripeJsInterface.openAuthenticatedWebView(
            ConnectJson.encodeToString(message)
        )
        verify(mockDelegate).onReceivedOpenAuthenticatedWebView(activity, message)
    }

    @Test
    fun `JS pageDidLoad is handled`() {
        val message = PageLoadMessage("page-id")
        webView.stripeJsInterface.pageDidLoad(ConnectJson.encodeToString(message))
        verify(mockDelegate).onReceivedPageDidLoad(message.pageViewId)
    }

    @Test
    fun `JS accountSessionClaimed is handled`() {
        val message = AccountSessionClaimedMessage("merchant-id")
        webView.stripeJsInterface.accountSessionClaimed(ConnectJson.encodeToString(message))
        verify(mockDelegate).onMerchantIdChanged(message.merchantId)
    }

    @Test
    fun `JS openFinancialConnections is handled`() = runTest {
        containerView.addView(webView)
        val message = OpenFinancialConnectionsMessage(
            id = "id",
            clientSecret = "client-secret",
            connectedAccountId = "connected-account-id",
        )
        webView.stripeJsInterface.openFinancialConnections(ConnectJson.encodeToString(message))
        verify(mockDelegate).onOpenFinancialConnections(activity, message)
    }

    @Test
    fun `JS fetchClientSecret is handled`() = runTest {
        val clientSecret = "client-secret"
        wheneverBlocking { mockDelegate.fetchClientSecret() }.doReturn(clientSecret)
        assertThat(webView.stripeJsInterface.fetchClientSecret())
            .isEqualTo(clientSecret)
    }

    @Test
    fun `JS closeWebView is handled`() {
        webView.stripeJsInterface.closeWebView()
        verify(mockDelegate).onCloseWebView()
    }

    @Test
    fun `context is updated when attachment changes`() {
        // We need the Activity context for some UI to work, like web-triggered dialogs
        fun assertWebViewContext(cls: Class<out Context>) {
            assertThat(webView.context.findRealContext()).isInstanceOf(cls)
        }

        assertWebViewContext(Application::class.java)

        containerView.addView(webView)
        assertWebViewContext(Activity::class.java)

        containerView.removeView(webView)
        assertWebViewContext(Application::class.java)
    }

    private fun Context.findRealContext(): Context? {
        return when (this) {
            is Activity, is Application -> this
            is ContextWrapper -> baseContext.findRealContext()
            else -> null
        }
    }
}
