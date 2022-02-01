package com.stripe.android.stripe3ds2.views

import android.net.Uri
import android.webkit.WebResourceRequest
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ThreeDS2WebViewClientTest {

    private val listener = mock<ThreeDS2WebViewClient.OnHtmlSubmitListener>()
    private val webViewClient = ThreeDS2WebViewClient().also {
        it.listener = listener
    }

    @Test
    fun shouldNotInterceptRequest() {
        assertThat(
            webViewClient.shouldNotInterceptUrl(Uri.parse("data:text/html,<h1>asdf</h1>"))
        ).isTrue()
        assertThat(
            webViewClient.shouldNotInterceptUrl(Uri.parse("data:image/png,base64encodedimage"))
        ).isTrue()
        assertThat(
            webViewClient.shouldNotInterceptUrl(Uri.parse("https://google.com/"))
        ).isFalse()
        assertThat(
            webViewClient.shouldNotInterceptUrl(Uri.parse("http://google.com/"))
        ).isFalse()
        verify(listener, never()).onHtmlSubmit(anyString())
    }

    @Test
    fun shouldOverrideUrlLoading_notForm_shouldOverrideNoCallback() {
        assertThat(
            webViewClient.shouldOverrideUrlLoading(
                mock(),
                FakeWebResourceRequest(Uri.parse("https://google.com/"))
            )
        ).isTrue()
        verify(listener, never())
            .onHtmlSubmit(anyString())
    }

    @Test
    fun shouldOverrideUrlLoading_form_shouldOverrideWithCallback() {
        assertThat(
            webViewClient.shouldOverrideUrlLoading(
                mock(),
                FAKE_WEB_RESOURCE_REQUEST
            )
        ).isTrue()
        verify(listener)
            .onHtmlSubmit("challenge=123456&submit=verify")
    }

    @Test
    fun shouldInterceptRequest_form_shouldOverrideWithCallback() {
        webViewClient.shouldInterceptRequest(mock(), FAKE_WEB_RESOURCE_REQUEST)
        verify(listener)
            .onHtmlSubmit("challenge=123456&submit=verify")
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

    private companion object {
        private val FAKE_WEB_RESOURCE_REQUEST =
            FakeWebResourceRequest(
                Uri.parse("https://emv3ds/challenge?challenge=123456&submit=verify")
            )
    }
}
