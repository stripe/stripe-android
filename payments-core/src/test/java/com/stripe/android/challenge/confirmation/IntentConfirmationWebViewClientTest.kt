package com.stripe.android.challenge.confirmation

import android.net.Uri
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationWebViewClientTest {

    // onReceivedError (API 23+) tests
    @Test
    fun `onReceivedError for host url calls errorHandler with correct details`() =
        testWithSetup { client, errors, webView ->
            val request = createRequest(HOST_URL)
            val error = createWebResourceError()

            client.onReceivedError(webView, request, error)

            // Note: The implementation calls super.onReceivedError() which triggers the legacy version too
            assertThat(errors.size).isEqualTo(1)
            errors[0].assertHasDetails(
                message = "net::ERR_FAILED",
                errorCode = -2,
                url = HOST_URL,
                type = "generic_resource_error"
            )
        }

    @Test
    fun `onReceivedError for wrong url does not call errorHandler`() = testWithSetup { client, errors, webView ->
        val request = createRequest(url = "https://example.com/iframe")
        val error = createWebResourceError()

        client.onReceivedError(webView, request, error)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `onReceivedError handles trailing slash in failingUrl`() = testWithSetup { client, errors, webView ->
        val request = createRequest(url = "$HOST_URL/")
        val error = createWebResourceError()

        client.onReceivedError(webView, request, error)

        assertThat(errors.size).isEqualTo(1)
        errors[0].assertHasDetails(
            message = "net::ERR_FAILED",
            errorCode = -2,
            url = "$HOST_URL/",
            type = "generic_resource_error"
        )
    }

    @Test
    fun `onReceivedError handles trailing slash in hostUrl`() =
        testWithSetup("$HOST_URL/") { client, errors, webView ->
            val request = createRequest(HOST_URL)
            val error = createWebResourceError()

            client.onReceivedError(webView, request, error)

            assertThat(errors.size).isEqualTo(1)
            errors[0].assertHasDetails(
                message = "net::ERR_FAILED",
                errorCode = -2,
                url = HOST_URL,
                type = "generic_resource_error"
            )
        }

    // onReceivedHttpError tests
    @Test
    fun `onReceivedHttpError for host url calls errorHandler`() = testWithSetup { client, errors, webView ->
        val request = createRequest(HOST_URL)
        val response = createWebResourceResponse()

        client.onReceivedHttpError(webView, request, response)

        assertThat(errors).hasSize(1)
        errors[0].assertHasDetails(
            message = "Not Found",
            errorCode = 404,
            url = HOST_URL,
            type = "http_error"
        )
    }

    @Test
    fun `onReceivedHttpError for non-host url does not call errorHandler`() = testWithSetup { client, errors, webView ->
        val request = createRequest(url = "https://example.com/iframe")
        val response = createWebResourceResponse()

        client.onReceivedHttpError(webView, request, response)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `onReceivedHttpError handles trailing slash in request url`() = testWithSetup { client, errors, webView ->
        val request = createRequest(url = "$HOST_URL/")
        val response = createWebResourceResponse()

        client.onReceivedHttpError(webView, request, response)

        assertThat(errors).hasSize(1)
        errors[0].assertHasDetails(
            message = "Not Found",
            errorCode = 404,
            url = "$HOST_URL/",
            type = "http_error"
        )
    }

    @Test
    fun `onReceivedHttpError handles trailing slash in hostUrl`() =
        testWithSetup(hostUrl = "$HOST_URL/") { client, errors, webView ->
            val request = createRequest(HOST_URL)
            val response = createWebResourceResponse()

            client.onReceivedHttpError(webView, request, response)

            assertThat(errors).hasSize(1)
            errors[0].assertHasDetails(
                message = "Not Found",
                errorCode = 404,
                url = HOST_URL,
                type = "http_error"
            )
        }

    // onReceivedSslError tests
    @Test
    fun `onReceivedSslError calls errorHandler with correct details`() = testWithSetup { client, errors, webView ->
        val handler = mock<SslErrorHandler>()
        val sslError = createSslError()

        client.onReceivedSslError(webView, handler, sslError)

        verify(handler, org.mockito.kotlin.atLeastOnce()).cancel()
        assertThat(errors).hasSize(1)
        errors[0].assertHasDetails(
            message = "received ssl error",
            errorCode = SslError.SSL_UNTRUSTED,
            url = HOST_URL,
            type = "ssl_error"
        )
    }

    // onRenderProcessGone tests
    @Test
    fun `onRenderProcessGone calls errorHandler with view URL`() = testWithSetup { client, errors, webView ->
        webView.loadUrl(HOST_URL)
        val detail = createRenderProcessGoneDetail()

        client.onRenderProcessGone(webView, detail)

        assertThat(errors).hasSize(1)
        assertThat(errors[0].message).isEqualTo("render process crashed")
        assertThat(errors[0].errorCode).isNull()
        assertThat(errors[0].webViewErrorType).isEqualTo("render_process_gone")
    }

    // Helper methods
    private fun testWithSetup(
        hostUrl: String = HOST_URL,
        block: (IntentConfirmationWebViewClient, MutableList<WebViewError>, WebView) -> Unit
    ) {
        val capturedErrors = mutableListOf<WebViewError>()
        val errorHandler = WebViewErrorHandler { error -> capturedErrors.add(error) }
        val client = IntentConfirmationWebViewClient(hostUrl, errorHandler = errorHandler)
        val webView = WebView(ApplicationProvider.getApplicationContext())

        block(client, capturedErrors, webView)
    }

    private fun createRequest(url: String): WebResourceRequest {
        return FakeWebResourceRequest(url)
    }

    private fun WebViewError.assertHasDetails(
        message: String?,
        errorCode: Int?,
        url: String?,
        type: String
    ) {
        assertThat(this.message).isEqualTo(message)
        assertThat(this.errorCode).isEqualTo(errorCode)
        assertThat(this.url).isEqualTo(url)
        assertThat(this.webViewErrorType).isEqualTo(type)
    }

    private fun createWebResourceError(): WebResourceError {
        return mock<WebResourceError>().apply {
            whenever(getErrorCode()).thenReturn(-2)
            whenever(getDescription()).thenReturn("net::ERR_FAILED")
        }
    }

    private fun createWebResourceResponse(): WebResourceResponse {
        return FakeWebResourceResponse(404, "Not Found")
    }

    private fun createSslError(): SslError {
        return mock<SslError>().apply {
            whenever(getPrimaryError()).thenReturn(SslError.SSL_UNTRUSTED)
            whenever(getUrl()).thenReturn(HOST_URL)
        }
    }

    // Fake implementations
    private class FakeWebResourceRequest(
        private val url: String,
    ) : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame(): Boolean = false
        override fun isRedirect(): Boolean = false
        override fun hasGesture(): Boolean = false
        override fun getMethod(): String = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

    private class FakeWebResourceResponse(
        private val statusCode: Int,
        private val reasonPhrase: String?
    ) : WebResourceResponse("text/html", "UTF-8", null) {
        override fun getStatusCode(): Int = statusCode
        override fun getReasonPhrase(): String? = reasonPhrase
    }

    private fun createRenderProcessGoneDetail(): RenderProcessGoneDetail {
        return FakeRenderProcessGoneDetail()
    }

    @Suppress("DEPRECATION")
    private class FakeRenderProcessGoneDetail : RenderProcessGoneDetail() {
        override fun didCrash(): Boolean = true
        override fun rendererPriorityAtExit(): Int = 0
    }

    companion object {
        private const val HOST_URL = "https://pay.stripe.com"
    }
}
