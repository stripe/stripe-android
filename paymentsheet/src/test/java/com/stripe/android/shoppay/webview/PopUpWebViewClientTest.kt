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
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PopUpWebViewClientTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

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
        val response = mock<WebResourceResponse>()
        whenever(env.assetLoader.shouldInterceptRequest(env.uri)).thenReturn(response)

        val result = env.client.shouldInterceptRequest(env.webView, env.request)

        assertThat(result).isEqualTo(response)
        verify(env.assetLoader).shouldInterceptRequest(env.uri)
    }

    @Test
    fun `onPageFinished calls onPageLoaded callback with correct parameters`() {
        val env = createTestEnvironment()
        val testUrl = "https://pay.stripe.com/test"

        env.client.onPageFinished(env.webView, testUrl)

        verify(env.onPageLoaded).invoke(env.webView, testUrl)
    }

    private fun createTestEnvironment() = TestEnvironment()

    private class TestEnvironment {
        val assetLoader = mock<WebViewAssetLoader>()
        val onPageLoaded: (WebView, String) -> Unit = mock()
        val webView = mock<WebView>()
        val request = mock<WebResourceRequest>()
        val uri = mock<Uri>()
        val client = PopUpWebViewClient(assetLoader, onPageLoaded)

        init {
            whenever(request.url).thenReturn(uri)
        }
    }
}
