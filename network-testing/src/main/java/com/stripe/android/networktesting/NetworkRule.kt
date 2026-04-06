package com.stripe.android.networktesting

import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.core.networking.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
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
    private var originalHttpClientFactory: HttpClientFactory? = null

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
        originalHttpClientFactory = ConnectionFactory.Default.httpClientFactory
        ConnectionFactory.Default.httpClientFactory = NetworkRuleHttpClientFactory()
    }

    private fun tearDown() {
        mockWebServer.dispatcher.clear()
        originalHttpClientFactory?.let {
            ConnectionFactory.Default.httpClientFactory = it
        }
    }

    inner class NetworkRuleHttpClientFactory : HttpClientFactory {
        override fun create(
            configure: HttpClientConfig<*>.() -> Unit
        ): HttpClient {
            val trustManager = mockWebServer.clientTrustManager()

            return HttpClient(OkHttp) {
                engine {
                    config {
                        addInterceptor { chain ->
                            val request = chain.request()
                            val originalHost = request.header("original-host") ?: request.url.host

                            if (!hostsToTrack.contains(originalHost)) {
                                throw RequestNotFoundException(
                                    "Test request attempted to reach a non test endpoint. " +
                                        "Url: ${request.url}"
                                )
                            }

                            val rewrittenRequest = request.newBuilder()
                                .header("original-host", originalHost)
                                .url(
                                    request.url.newBuilder()
                                        .scheme(mockWebServer.baseUrl.scheme)
                                        .host(mockWebServer.baseUrl.host)
                                        .port(mockWebServer.baseUrl.port)
                                        .build()
                                )
                                .build()

                            chain.proceed(rewrittenRequest)
                        }
                        sslSocketFactory(
                            mockWebServer.clientSocketFactory(trustManager),
                            trustManager
                        )
                    }
                }
                configure()
            }
        }
    }
}

private fun String.hostFromUrl(): String = toHttpUrl().host
