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
    fun `shouldOverrideUrlLoading returns false and delegates to super`() {
        val mockAssetLoader = mock<WebViewAssetLoader>()
        val mockCallback: (WebView, String) -> Unit = mock()
        val mockWebView = mock<WebView>()
        val mockRequest = mock<WebResourceRequest>()
        val mockUri = mock<Uri>()

        whenever(mockRequest.url).thenReturn(mockUri)
        whenever(mockUri.toString()).thenReturn("https://example.com")

        val client = PopUpWebViewClient(
            assetLoader = mockAssetLoader,
            onPageLoaded = mockCallback
        )

        val result = client.shouldOverrideUrlLoading(mockWebView, mockRequest)

        assertThat(result).isFalse()
    }

    @Test
    fun `shouldInterceptRequest returns null when request is null`() {
        val mockAssetLoader = mock<WebViewAssetLoader>()
        val mockCallback: (WebView, String) -> Unit = mock()
        val mockWebView = mock<WebView>()

        val client = PopUpWebViewClient(
            assetLoader = mockAssetLoader,
            onPageLoaded = mockCallback
        )

        val result = client.shouldInterceptRequest(mockWebView, null as WebResourceRequest?)

        assertThat(result).isNull()
        verify(mockAssetLoader, never()).shouldInterceptRequest(any<Uri>())
    }

    @Test
    fun `shouldInterceptRequest delegates to assetLoader when request is not null`() {
        val mockAssetLoader = mock<WebViewAssetLoader>()
        val mockCallback: (WebView, String) -> Unit = mock()
        val mockWebView = mock<WebView>()
        val mockRequest = mock<WebResourceRequest>()
        val mockUri = mock<Uri>()
        val mockResponse = mock<WebResourceResponse>()

        whenever(mockRequest.url).thenReturn(mockUri)
        whenever(mockAssetLoader.shouldInterceptRequest(mockUri)).thenReturn(mockResponse)

        val client = PopUpWebViewClient(
            assetLoader = mockAssetLoader,
            onPageLoaded = mockCallback
        )

        val result = client.shouldInterceptRequest(mockWebView, mockRequest)

        assertThat(result).isEqualTo(mockResponse)
        verify(mockAssetLoader).shouldInterceptRequest(mockUri)
    }

    @Test
    fun `shouldInterceptRequest returns null when assetLoader returns null`() {
        val mockAssetLoader = mock<WebViewAssetLoader>()
        val mockCallback: (WebView, String) -> Unit = mock()
        val mockWebView = mock<WebView>()
        val mockRequest = mock<WebResourceRequest>()
        val mockUri = mock<Uri>()

        whenever(mockRequest.url).thenReturn(mockUri)
        whenever(mockAssetLoader.shouldInterceptRequest(mockUri)).thenReturn(null)

        val client = PopUpWebViewClient(
            assetLoader = mockAssetLoader,
            onPageLoaded = mockCallback
        )

        val result = client.shouldInterceptRequest(mockWebView, mockRequest)

        assertThat(result).isNull()
        verify(mockAssetLoader).shouldInterceptRequest(mockUri)
    }

    @Test
    fun `onPageFinished calls onPageLoaded callback with correct parameters`() {
        val mockAssetLoader = mock<WebViewAssetLoader>()
        val mockCallback: (WebView, String) -> Unit = mock()
        val mockWebView = mock<WebView>()
        val testUrl = "https://pay.stripe.com/test"

        val client = PopUpWebViewClient(
            assetLoader = mockAssetLoader,
            onPageLoaded = mockCallback
        )

        client.onPageFinished(mockWebView, testUrl)

        verify(mockCallback).invoke(mockWebView, testUrl)
    }
}
