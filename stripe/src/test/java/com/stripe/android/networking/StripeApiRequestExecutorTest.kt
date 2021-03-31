package com.stripe.android.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.net.UnknownHostException
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class StripeApiRequestExecutorTest {
    private val testDispatcher = TestCoroutineDispatcher()

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun bodyBytes_shouldHandleUnsupportedEncodingException() {
        val stripeRequest = object : StripeRequest() {
            override val method: Method = Method.POST
            override val baseUrl: String = ApiRequest.API_HOST
            override val params: Map<String, *>? = null
            override val mimeType: MimeType = MimeType.Form
            override val headersFactory = RequestHeadersFactory.Fingerprint(
                UUID.randomUUID().toString()
            )

            override val body: String
                get() {
                    throw UnsupportedEncodingException()
                }
        }

        assertFailsWith<InvalidRequestException> {
            ByteArrayOutputStream().use {
                stripeRequest.writeBody(it)
            }
        }
    }

    @Test
    fun `executeInternal with IllegalStateException should throw the exception`() =
        testDispatcher.runBlockingTest {
            val executor = ApiRequestExecutor.Default(
                connectionFactory = FakeConnectionFactory(
                    FailingConnection(
                        IllegalStateException("Failure")
                    )
                )
            )

            val failure = assertFailsWith<IllegalStateException> {
                executor.executeInternal(FakeStripeRequest(), MAX_RETRIES)
            }
            assertThat(failure.message)
                .isEqualTo("Failure")
        }

    @Test
    fun `executeInternal with IOException should throw an APIConnectionException`() =
        testDispatcher.runBlockingTest {
            val executor = ApiRequestExecutor.Default(
                connectionFactory = FakeConnectionFactory(
                    FailingConnection(
                        UnknownHostException("Could not connect to Stripe API")
                    )
                )
            )

            val failure = assertFailsWith<APIConnectionException> {
                executor.executeInternal(FakeStripeRequest(), MAX_RETRIES)
            }
            assertThat(failure.message)
                .isEqualTo("IOException during API request to Stripe (https://api.stripe.com): Could not connect to Stripe API. Please check your internet connection and try again. If this problem persists, you should check Stripe's service status at https://twitter.com/stripestatus, or let us know at support@stripe.com.")
        }

    @Test
    fun `executeInternal when retries exhausted should return rate-limited response`() =
        testDispatcher.runBlockingTest {
            val connectionFactory = FakeConnectionFactory(
                FakeConnection(429)
            )
            val executor = ApiRequestExecutor.Default(
                connectionFactory = connectionFactory,
                retryDelaySupplier = RetryDelaySupplier(0)
            )

            val response = executor.executeInternal(FakeStripeRequest(), MAX_RETRIES)
            assertThat(connectionFactory.createInvocations)
                .isEqualTo(MAX_RETRIES + 1)

            assertThat(response.isRateLimited)
                .isTrue()
        }

    @Test
    fun `executeInternal when rate-limited once then succeeds should return OK response`() =
        testDispatcher.runBlockingTest {
            val connectionFactory = FakeConnectionFactory { count ->
                if (count <= 1) {
                    FakeConnection(429)
                } else {
                    FakeConnection(200)
                }
            }
            val executor = ApiRequestExecutor.Default(
                connectionFactory = connectionFactory,
                retryDelaySupplier = RetryDelaySupplier(0)
            )

            val response = executor.executeInternal(FakeStripeRequest(), MAX_RETRIES)
            assertThat(connectionFactory.createInvocations)
                .isEqualTo(2)

            assertThat(response.isOk)
                .isTrue()
        }

    private class FakeStripeRequest : StripeRequest() {
        override val method: Method = Method.POST
        override val baseUrl: String = ApiRequest.API_HOST
        override val params: Map<String, *>? = null
        override val mimeType: MimeType = MimeType.Form
        override val headersFactory = RequestHeadersFactory.Fingerprint(
            UUID.randomUUID().toString()
        )

        override val body: String = ""
    }

    private class FakeConnectionFactory(
        private val stripeConnection: (Int) -> StripeConnection
    ) : ConnectionFactory {
        constructor(
            stripeConnection: StripeConnection
        ) : this(
            { stripeConnection }
        )

        var createInvocations = 0

        override fun create(
            request: StripeRequest
        ): StripeConnection {
            createInvocations++
            return stripeConnection(createInvocations)
        }
    }

    private class FailingConnection(
        private val error: Throwable
    ) : StripeConnection {
        override val responseCode: Int = 500

        override val response: StripeResponse
            get() {
                throw error
            }

        override fun close() {
        }
    }

    private class FakeConnection(
        override val responseCode: Int
    ) : StripeConnection {
        override val response: StripeResponse
            get() = StripeResponse(
                code = responseCode,
                body = null
            )

        override fun close() {
        }
    }

    private companion object {
        private const val MAX_RETRIES = 3
    }
}
