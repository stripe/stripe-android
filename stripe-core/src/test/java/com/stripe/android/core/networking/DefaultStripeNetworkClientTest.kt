package com.stripe.android.core.networking

import android.net.http.HttpResponseCache
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.InputStream
import java.net.CacheRequest
import java.net.CacheResponse
import java.net.HttpURLConnection
import java.net.ResponseCache
import java.net.URI
import java.net.URLConnection
import java.net.UnknownHostException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DefaultStripeNetworkClientTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val okResponseWithString =
        StripeResponse(code = HttpURLConnection.HTTP_OK, body = "response_string")

    private val okResponseWithFile =
        StripeResponse(code = HttpURLConnection.HTTP_OK, body = File("response_file"))

    private val okConnectionFactory = mock<ConnectionFactory>()

    private val mockLogger = mock<Logger>()

    @BeforeTest
    fun setUp() {
        val okConnectionWithString = mock<StripeConnection<String>>()
        val okConnectionWithFile = mock<StripeConnection<File>>()

        whenever(okConnectionFactory.create(any())).thenReturn(okConnectionWithString)
        whenever(okConnectionFactory.createForFile(any(), any())).thenReturn(okConnectionWithFile)
        whenever(okConnectionWithString.response).thenReturn(okResponseWithString)
        whenever(okConnectionWithFile.response).thenReturn(okResponseWithFile)
    }

    @Test
    fun `executeRequest should return StripeResponse with String`() =
        runTest {
            val executor = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = okConnectionFactory
            )
            assertThat(executor.executeRequest(mock())).isSameInstanceAs(
                okResponseWithString
            )
        }

    @Test
    fun `executeRequestForFile should return StripeResponse with File`() =
        runTest {
            val executor = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = okConnectionFactory
            )
            assertThat(executor.executeRequestForFile(mock(), mock())).isSameInstanceAs(
                okResponseWithFile
            )
        }

    @Test
    fun `executeRequest with IllegalStateException should throw the exception`() =
        runTest {
            val client = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = RetryCountConnectionFactory(
                    FailingConnection(
                        IllegalStateException("Failure")
                    )
                )
            )

            val failure = assertFailsWith<IllegalStateException> {
                client.executeRequest(mock())
            }
            assertThat(failure.message)
                .isEqualTo("Failure")
        }

    @Test
    fun `executeRequest with IOException should throw an APIConnectionException`() =
        runTest {
            val exception = UnknownHostException("Could not connect to Stripe API")
            val client = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = RetryCountConnectionFactory(
                    FailingConnection(exception)
                ),
                logger = mockLogger
            )

            val failure = assertFailsWith<APIConnectionException> {
                client.executeRequest(FakeStripeRequest())
            }

            verify(mockLogger).error(
                eq("Exception while making Stripe API request"),
                same(exception)
            )
            assertThat(failure.message)
                .isEqualTo(
                    "IOException during API request to Stripe " +
                        "($TEST_HOST): Could not connect to Stripe API. Please check " +
                        "your internet connection and try again. If this problem persists, you " +
                        "should check Stripe's service status at " +
                        "https://twitter.com/stripestatus, or let us know at support@stripe.com."
                )
        }

    @Test
    fun `executeRequest when retries exhausted should return rate-limited response`() =
        runTest {
            val connectionFactory = RetryCountConnectionFactory(
                ResponseCodeOverrideConnection(HTTP_TOO_MANY_REQUESTS)
            )
            val client = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = connectionFactory,
                retryDelaySupplier = ExponentialBackoffRetryDelaySupplier(Duration.ZERO)
            )

            val response = client.executeRequest(FakeStripeRequest())
            assertThat(connectionFactory.createInvocations)
                .isEqualTo(MAX_RETRIES + 1)
            assertThat(response.isRateLimited)
                .isTrue()
        }

    @Test
    fun `executeRequest when retry code is returned once then succeeds should return OK response`() =
        runTest {
            val connectionFactory = RetryCountConnectionFactory { count ->
                if (count <= 1) {
                    ResponseCodeOverrideConnection(TEST_RETRY_CODES_START)
                } else {
                    ResponseCodeOverrideConnection(HttpURLConnection.HTTP_OK)
                }
            }
            val client = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = connectionFactory,
                retryDelaySupplier = ExponentialBackoffRetryDelaySupplier(Duration.ZERO),
                logger = mockLogger
            )

            val response = client.executeRequest(FakeStripeRequest())
            assertThat(connectionFactory.createInvocations).isEqualTo(2)
            verify(mockLogger).info(
                "Request failed with code $TEST_RETRY_CODES_START. Retrying up to 3 more time(s)."
            )
            assertThat(response.isOk).isTrue()
        }

    @Test
    fun `executeRequest when non retry code should not retry and return response with the code`() =
        runTest {
            val connectionFactory = RetryCountConnectionFactory(
                ResponseCodeOverrideConnection(TEST_NON_RETRY_CODES_END)
            )
            val executor = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = connectionFactory,
                retryDelaySupplier = ExponentialBackoffRetryDelaySupplier(Duration.ZERO)
            )

            val response = executor.executeRequest(FakeStripeRequest())
            assertThat(connectionFactory.createInvocations)
                .isEqualTo(1)

            assertThat(response.code).isEqualTo(TEST_NON_RETRY_CODES_END)
        }

    @Test
    fun `request that should cache creates a connection that uses cache`() = runTest {
        val server = MockWebServer()

        server.enqueue(MockResponse().setBody("response1"))
        server.enqueue(MockResponse().setBody("response2"))
        server.enqueue(MockResponse().setBody("response3"))

        server.start()

        var getCacheCount = 0
        var putCacheCount = 0
        HttpResponseCache.setDefault(object : ResponseCache() {
            override fun get(
                uri: URI?,
                rqstMethod: String?,
                rqstHeaders: MutableMap<String, MutableList<String>>?
            ): CacheResponse? {
                getCacheCount++
                return null
            }

            override fun put(uri: URI?, conn: URLConnection?): CacheRequest? {
                putCacheCount++
                return null
            }
        })

        val executor = DefaultStripeNetworkClient(
            workContext = testDispatcher,
            connectionFactory = ConnectionFactory.Default
        )

        val url = server.url("").toString()

        val requestWithCache = FakeStripeRequest(
            shouldCache = true,
            method = StripeRequest.Method.GET,
            url = url
        )

        val requestWithoutCache = FakeStripeRequest(
            shouldCache = false,
            method = StripeRequest.Method.GET,
            url = url
        )

        executor.executeRequest(requestWithCache)
        assertThat(getCacheCount).isEqualTo(1)
        assertThat(putCacheCount).isEqualTo(1)

        executor.executeRequest(requestWithCache)
        assertThat(getCacheCount).isEqualTo(2)
        assertThat(putCacheCount).isEqualTo(2)

        executor.executeRequest(requestWithoutCache)
        assertThat(getCacheCount).isEqualTo(2)
        assertThat(putCacheCount).isEqualTo(2)

        server.shutdown()
    }

    private class FakeStripeRequest(
        override val url: String = TEST_HOST,
        override val shouldCache: Boolean = false,
        override val method: Method = Method.POST,
        override val mimeType: MimeType = MimeType.Form,
        override val headers: Map<String, String> = emptyMap(),
        override val retryResponseCodes: Iterable<Int> = TEST_RETRY_CODES
    ) : StripeRequest()

    private class RetryCountConnectionFactory(
        private val stripeConnection: (Int) -> StripeConnection<String>
    ) : ConnectionFactory {
        constructor(
            stripeConnection: StripeConnection<String>
        ) : this(
            { stripeConnection }
        )

        var createInvocations = 0

        override fun create(
            request: StripeRequest
        ): StripeConnection<String> {
            createInvocations++
            return stripeConnection(createInvocations)
        }

        override fun createForFile(
            request: StripeRequest,
            outputFile: File
        ): StripeConnection<File> {
            // not used
            return mock()
        }
    }

    private class FailingConnection(
        private val error: Throwable
    ) : StripeConnection<String> {
        override val responseCode: Int = 500

        override val response: StripeResponse<String>
            get() {
                throw error
            }

        override fun close() {
        }

        override fun createBodyFromResponseStream(responseStream: InputStream?): String? = null
    }

    private class ResponseCodeOverrideConnection(
        override val responseCode: Int
    ) : StripeConnection<String> {
        override val response: StripeResponse<String>
            get() = StripeResponse(
                code = responseCode,
                body = null
            )

        override fun close() {
        }

        override fun createBodyFromResponseStream(responseStream: InputStream?): String? = null
    }

    private companion object {
        private const val MAX_RETRIES = 3
        private const val TEST_RETRY_CODES_START = 401
        private const val TEST_RETRY_CODES_END = 456
        private const val TEST_NON_RETRY_CODES_END = 457
        private const val TEST_HOST = "https://test.host.com"

        // [HTTP_TOO_MANY_REQUESTS] is included in this range
        private val TEST_RETRY_CODES: Iterable<Int> = TEST_RETRY_CODES_START..TEST_RETRY_CODES_END
    }
}
