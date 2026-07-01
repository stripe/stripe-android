package com.stripe.android.networktesting

import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.ConnectionFactory
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.json.JSONException
import org.json.JSONObject
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import javax.net.ssl.SSLSocketFactory
import kotlin.time.Duration

class NetworkRule private constructor(
    private val hostsToTrack: Set<String>,
    validationTimeout: Duration?,
) : TestRule {
    private val mockWebServer = TestMockWebServer(validationTimeout)

    val baseUrl: HttpUrl
        get() = mockWebServer.baseUrl

    constructor(
        hostsToTrack: List<String> = listOf(ApiRequest.API_HOST),
        validationTimeout: Duration? = null,
    ) : this(hostsToTrack.map { it.hostFromUrl() }.toSet(), validationTimeout)

    override fun apply(base: Statement, description: Description): Statement {
        return NetworkStatement(
            base,
            mockWebServer,
            hostsToTrack,
        )
    }

    fun clientSocketFactory(trustAll: Boolean = false): SSLSocketFactory {
        return mockWebServer.clientSocketFactory(trustAll)
    }

    fun enqueue(
        vararg requestMatcher: RequestMatcher,
        ensureResponseIsValidJson: Boolean = true,
        responseFactory: (MockResponse) -> Unit
    ) {
        mockWebServer.dispatcher.enqueue(*requestMatcher) { response ->
            responseFactory(response)
            if (ensureResponseIsValidJson) {
                assertResponseBodyIsValidJson(response)
            }
        }
    }

    fun enqueue(
        vararg requestMatcher: RequestMatcher,
        ensureResponseIsValidJson: Boolean = true,
        responseFactory: (TestRecordedRequest, MockResponse) -> Unit
    ) {
        mockWebServer.dispatcher.enqueue(*requestMatcher) { request, response ->
            responseFactory(request, response)
            if (ensureResponseIsValidJson) {
                assertResponseBodyIsValidJson(response)
            }
        }
    }

    fun validate() {
        mockWebServer.dispatcher.validate()
    }

    private fun assertResponseBodyIsValidJson(response: MockResponse) {
        try {
            response.getBody()?.readUtf8()?.let { bodyString ->
                JSONObject(bodyString)
            }
        } catch (_: JSONException) {
            // MockWebServer catches the exception so we need an error to fail the test
            throw AssertionError("Parsing JSON failed")
        }
    }
}

private class NetworkStatement(
    private val baseStatement: Statement,
    private val mockWebServer: TestMockWebServer,
    private val hostsToTrack: Set<String>,
) : Statement() {
    private var originalOkHttpClient: OkHttpClient? = null

    override fun evaluate() {
        try {
            if (
                !hostsToTrack.contains(AnalyticsRequest.HOST.hostFromUrl()) &&
                !hostsToTrack.contains(AnalyticsRequestV2.ANALYTICS_HOST.hostFromUrl())
            ) {
                AnalyticsRequestExecutor.ENABLED = false
            }
            setup()
            baseStatement.evaluate()
            mockWebServer.dispatcher.validate()
        } finally {
            AnalyticsRequestExecutor.ENABLED = true
            tearDown()
        }
    }

    private fun setup() {
        originalOkHttpClient = ConnectionFactory.Default.okHttpClient
        val trustManager = mockWebServer.clientTrustManager()
        val socketFactory = mockWebServer.clientSocketFactory()
        val mockBaseUrl = mockWebServer.baseUrl

        ConnectionFactory.Default.okHttpClient = ConnectionFactory.Default.buildDefaultClient()
            .newBuilder()
            .sslSocketFactory(socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestHost = originalRequest.url.host

                if (!hostsToTrack.contains(requestHost)) {
                    throw RequestNotFoundException(
                        "Test request attempted to reach a non test endpoint. " +
                            "Url: ${originalRequest.url}"
                    )
                }

                val redirectedUrl = originalRequest.url.newBuilder()
                    .scheme(mockBaseUrl.scheme)
                    .host(mockBaseUrl.host)
                    .port(mockBaseUrl.port)
                    .build()

                val redirectedRequest = originalRequest.newBuilder()
                    .url(redirectedUrl)
                    .header("original-host", requestHost)
                    .build()

                chain.proceed(redirectedRequest)
            }
            .build()
    }

    private fun tearDown() {
        mockWebServer.dispatcher.clear()
        originalOkHttpClient?.let {
            ConnectionFactory.Default.okHttpClient = it
        }
        originalOkHttpClient = null
    }
}

private fun String.hostFromUrl(): String = toHttpUrl().host
