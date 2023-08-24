package com.stripe.android.networktesting

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.core.networking.StripeRequest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.OutputStream
import javax.net.ssl.HttpsURLConnection

class NetworkRule : TestRule {
    private val mockWebServer = TestMockWebServer()

    override fun apply(base: Statement, description: Description): Statement {
        return NetworkStatement(base, description, mockWebServer)
    }

    fun enqueue(
        vararg requestMatcher: RequestMatcher,
        responseFactory: (MockResponse) -> Unit
    ) {
        mockWebServer.dispatcher.enqueue(*requestMatcher) { response ->
            responseFactory(response)
        }
    }

    fun enqueue(
        vararg requestMatcher: RequestMatcher,
        responseFactory: (TestRecordedRequest, MockResponse) -> Unit
    ) {
        mockWebServer.dispatcher.enqueue(*requestMatcher) { request, response ->
            responseFactory(request, response)
        }
    }
}

private class NetworkStatement(
    private val baseStatement: Statement,
    private val description: Description,
    private val mockWebServer: TestMockWebServer,
) : Statement() {
    override fun evaluate() {
        try {
            setup()
            baseStatement.evaluate()
            validate()
        } finally {
            tearDown()
        }
    }

    private fun setup() {
        ConnectionFactory.Default.testHostCustomization = lambda@{ request ->
            if (!request.url.startsWith(ApiRequest.API_HOST)) {
                throw RequestNotFoundException(
                    "Test request attempted to reach a non test endpoint. " +
                        "Url: ${request.url}"
                )
            }

            return@lambda DelegatingStripeRequest(
                request, request.url.replace(
                    ApiRequest.API_HOST,
                    mockWebServer.baseUrl.toString().removeSuffix("/")
                )
            )
        }
        ConnectionFactory.Default.testConnectionCustomization = lambda@{ insecureConnection ->
            val connection = insecureConnection as? HttpsURLConnection ?: return@lambda
            connection.sslSocketFactory = mockWebServer.clientSocketFactory()
        }
    }

    private fun validate() {
        val numberRemainingInQueue = mockWebServer.dispatcher.numberRemainingInQueue()
        if (numberRemainingInQueue != 0) {
            throw IllegalStateException(
                "${description.testClass}#${description.methodName} - mock responses is not " +
                    "empty. Remaining: $numberRemainingInQueue.\nRemaining Matchers: " +
                    mockWebServer.dispatcher.remainingMatchersDescription()
            )
        }
    }

    private fun tearDown() {
        mockWebServer.dispatcher.clear()
        ConnectionFactory.Default.testConnectionCustomization = null
    }
}

private class DelegatingStripeRequest(
    private val original: StripeRequest,
    private val testUrl: String,
) : StripeRequest() {

    private val originalHost: String = original.url.toHttpUrl().host

    override val method: Method
        get() = original.method

    override val mimeType: MimeType
        get() = original.mimeType

    override val retryResponseCodes: Iterable<Int>
        get() = original.retryResponseCodes

    override val url: String
        get() = testUrl

    override val headers: Map<String, String>
        get() = original.headers.plus(Pair("original-host", originalHost))

    override var postHeaders: Map<String, String>? = original.postHeaders

    override val shouldCache: Boolean
        get() = original.shouldCache

    override fun writePostBody(outputStream: OutputStream) {
        original.writePostBody(outputStream)
    }
}
