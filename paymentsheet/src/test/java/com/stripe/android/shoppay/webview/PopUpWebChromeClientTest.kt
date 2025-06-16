package com.stripe.android.shoppay.webview

import android.content.Context
import android.os.Message
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.webkit.WebViewAssetLoader
import com.google.common.truth.Truth.assertThat
import com.stripe.android.shoppay.bridge.BridgeHandler
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PopUpWebChromeClientTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `onCreateWindow returns false when resultMsg is null`() {
        val client = createClient()
        val mockWebView = mock<WebView>()

        val result = client.onCreateWindow(mockWebView, false, true, null)

        assertThat(result).isFalse()
    }

    @Test
    fun `onCreateWindow returns false when transport is not WebViewTransport`() {
        val client = createClient()
        val mockWebView = mock<WebView>()
        val mockMessage = mock<Message>()
        val nonTransportObject = "not a transport"

        whenever(mockMessage.obj).thenReturn(nonTransportObject)

        val result = client.onCreateWindow(mockWebView, false, true, mockMessage)

        assertThat(result).isFalse()
    }

    @Test
    fun `onCloseWindow calls closeWebView callback`() {
        val mockCloseWebView: () -> Unit = mock()
        val client = createClient(closeWebView = mockCloseWebView)
        val mockWebView = mock<WebView>()

        client.onCloseWindow(mockWebView)

        verify(mockCloseWebView).invoke()
    }

    private fun createClient(
        context: Context = ApplicationProvider.getApplicationContext(),
        bridgeHandler: BridgeHandler = mock(),
        assetLoader: WebViewAssetLoader = mock(),
        setPopUpView: (WebView) -> Unit = mock(),
        closeWebView: () -> Unit = mock(),
        onPageLoaded: (WebView, String) -> Unit = mock()
    ): PopUpWebChromeClient {
        return PopUpWebChromeClient(
            context = context,
            bridgeHandler = bridgeHandler,
            assetLoader = assetLoader,
            setPopUpView = setPopUpView,
            closeWebView = closeWebView,
            onPageLoaded = onPageLoaded
        )
    }
}
