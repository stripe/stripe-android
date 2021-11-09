package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.RequestHeadersFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
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
import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DefaultStripeNetworkClientTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val okResponseWithString =
        StripeResponse(code = HttpURLConnection.HTTP_OK, body = "response_string")

    private val okResponseWithFile =
        StripeResponse(code = HttpURLConnection.HTTP_OK, body = File("response_file"))

    private val okConnectionFactory = mock<ConnectionFactory>()

    private val mockLogger = mock<Logger>()

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

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
        testDispatcher.runBlockingTest {
            val executor = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = okConnectionFactory,
            )
            assertThat(executor.executeRequest(mock())).isSameInstanceAs(
                okResponseWithString
            )
        }

    @Test
    fun `executeRequestForFile should return StripeResponse with File`() =
        testDispatcher.runBlockingTest {
            val executor = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = okConnectionFactory,
            )
            assertThat(executor.executeRequestForFile(mock(), mock())).isSameInstanceAs(
                okResponseWithFile
            )
        }

    @Test
    fun `executeRequest with IllegalStateException should throw the exception`() =
        testDispatcher.runBlockingTest {

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
        testDispatcher.runBlockingTest {
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
                        "(https://api.stripe.com): Could not connect to Stripe API. Please check " +
                        "your internet connection and try again. If this problem persists, you " +
                        "should check Stripe's service status at " +
                        "https://twitter.com/stripestatus, or let us know at support@stripe.com."
                )
        }

    @Test
    fun `executeRequest when retries exhausted should return rate-limited response`() =
        testDispatcher.runBlockingTest {
            val connectionFactory = RetryCountConnectionFactory(
                ResponseCodeOverrideConnection(HTTP_TOO_MANY_REQUESTS)
            )
            val client = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = connectionFactory,
                retryDelaySupplier = RetryDelaySupplier(0)
            )

            val response = client.executeRequest(FakeStripeRequest())
            assertThat(connectionFactory.createInvocations)
                .isEqualTo(MAX_RETRIES + 1)
            assertThat(response.isRateLimited)
                .isTrue()
        }

    @Test
    fun `executeRequest when retry code is returned once then succeeds should return OK response`() =
        testDispatcher.runBlockingTest {
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
                retryDelaySupplier = RetryDelaySupplier(0),
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
        testDispatcher.runBlockingTest {
            val connectionFactory = RetryCountConnectionFactory(
                ResponseCodeOverrideConnection(TEST_NON_RETRY_CODES_END)
            )
            val executor = DefaultStripeNetworkClient(
                workContext = testDispatcher,
                connectionFactory = connectionFactory,
                retryDelaySupplier = RetryDelaySupplier(0)
            )

            val response = executor.executeRequest(FakeStripeRequest())
            assertThat(connectionFactory.createInvocations)
                .isEqualTo(1)

            assertThat(response.code).isEqualTo(TEST_NON_RETRY_CODES_END)
        }

    private class FakeStripeRequest : StripeRequest() {
        override val method: Method = Method.POST
        override val url: String = ApiRequest.API_HOST
        override val mimeType: MimeType = MimeType.Form
        override val headers = RequestHeadersFactory.FraudDetection(
            UUID.randomUUID().toString()
        ).create()
        override val retryResponseCodes = TEST_RETRY_CODES
    }

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

        // [HTTP_TOO_MANY_REQUESTS] is included in this range
        private val TEST_RETRY_CODES: Iterable<Int> = TEST_RETRY_CODES_START..TEST_RETRY_CODES_END
    }
}
