package com.stripe.android.networktesting

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.ConnectionFactory
import okhttp3.mockwebserver.MockResponse
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
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
        ApiRequest.API_TEST_HOST.set(mockWebServer.baseUrl.toString().removeSuffix("/"))
        ConnectionFactory.Default.testConnectionCustomization.set { insecureConnection ->
            if (mockWebServer.baseUrl.host != insecureConnection.url.host) {
                throw RequestNotFoundException(
                    "Test request attempted to reach a non test endpoint. " +
                        "Url: ${insecureConnection.url}"
                )
            }
            val connection = insecureConnection as? HttpsURLConnection ?: return@set
            connection.sslSocketFactory = mockWebServer.clientSocketFactory()
        }
    }

    private fun validate() {
        val numberRemainingInQueue = mockWebServer.dispatcher.numberRemainingInQueue()
        if (numberRemainingInQueue != 0) {
            throw IllegalStateException(
                "${description.testClass}#${description.methodName} - mock responses is not " +
                    "empty. Remaining: $numberRemainingInQueue."
            )
        }
    }

    private fun tearDown() {
        mockWebServer.dispatcher.clear()
        ConnectionFactory.Default.testConnectionCustomization.set(null)
        ApiRequest.API_TEST_HOST.set(null)
    }
}
