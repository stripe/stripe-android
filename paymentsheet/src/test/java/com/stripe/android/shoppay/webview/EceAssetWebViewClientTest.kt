package com.stripe.android.shoppay.webview

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
internal class EceAssetWebViewClientTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `shouldOverrideUrlLoading returns false when asset loader supports request`() {
        val response = WebResourceResponse("type/png", "", InputStream.nullInputStream())
        val env = createTestEnvironment(response)

        val uri = Uri.parse("https://pay.stripe.com")
        val result = env.client.shouldOverrideUrlLoading(env.webView, FakeWebResourceRequest(uri))

        assertThat(result).isFalse()
        verify(env.assetLoader, times(1)).shouldInterceptRequest(uri)
    }

    @Test
    fun `shouldOverrideUrlLoading returns true when asset loader does not support request`() {
        val env = createTestEnvironment()

        val uri = Uri.parse("https://notvalid.stripe.com")
        val result = env.client.shouldOverrideUrlLoading(env.webView, FakeWebResourceRequest(uri))

        assertThat(result).isTrue()
    }

    @Test
    fun `shouldOverrideUrlLoading returns true when request is null`() {
        val env = createTestEnvironment()

        val result = env.client.shouldOverrideUrlLoading(
            env.webView,
            null as WebResourceRequest?
        )

        assertThat(result).isTrue()
        verify(env.assetLoader, never()).shouldInterceptRequest(any<Uri>())
    }

    @Test
    fun `shouldInterceptRequest returns null when request is null`() {
        val env = createTestEnvironment()

        val result = env.client.shouldInterceptRequest(env.webView, null as WebResourceRequest?)

        assertThat(result).isNull()
        verify(env.assetLoader, never()).shouldInterceptRequest(any<Uri>())
    }

    @Test
    fun `shouldInterceptRequest delegates to assetLoader when request is not null`() {
        val env = createTestEnvironment()
        val response = WebResourceResponse("text/html", "utf-8", InputStream.nullInputStream())
        val uri = Uri.parse("https://pay.stripe.com")
        val request = FakeWebResourceRequest(uri)

        whenever(env.assetLoader.shouldInterceptRequest(uri))
            .thenReturn(response)

        val result = env.client.shouldInterceptRequest(env.webView, request)

        assertThat(result).isEqualTo(response)
        verify(env.assetLoader).shouldInterceptRequest(uri)
    }

    @Test
    fun `onPageFinished calls onPageLoaded callback with correct parameters`() {
        val env = createTestEnvironment()
        val testUrl = "https://pay.stripe.com/test"

        env.client.onPageFinished(env.webView, testUrl)

        verify(env.onPageLoaded).invoke(env.webView, testUrl)
    }

    private fun createTestEnvironment(
        response: WebResourceResponse? = null
    ) = TestEnvironment(response)

    private class TestEnvironment(
        val response: WebResourceResponse?,
    ) {
        val onPageLoaded: (WebView, String) -> Unit = mock()
        val assetLoader = mock<WebViewAssetLoader> {
            on {
                shouldInterceptRequest(any())
            } doReturn response
        }
        val webView = mock<WebView>()
        val client = EceAssetWebViewClient(assetLoader, onPageLoaded)
    }

    private class FakeWebResourceRequest(
        private val uri: Uri
    ) : WebResourceRequest {
        override fun getUrl(): Uri = uri

        override fun isForMainFrame(): Boolean = false

        override fun isRedirect(): Boolean = false

        override fun hasGesture(): Boolean = false

        override fun getMethod(): String = "GET"

        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }
}
