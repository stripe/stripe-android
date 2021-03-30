package com.stripe.android.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.net.UnknownHostException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class StripeApiRequestExecutorTest {
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
    fun `executeInternal with IllegalStateException should throw the exception`() {
        val executor = ApiRequestExecutor.Default(
            connectionFactory = FakeConnectionFactory(
                FailingConnection(
                    IllegalStateException("Failure")
                )
            )
        )

        val failure = assertFailsWith<IllegalStateException> {
            executor.executeInternal(FakeStripeRequest())
        }
        assertThat(failure.message)
            .isEqualTo("Failure")
    }

    @Test
    fun `executeInternal with IOException should throw an APIConnectionException`() {
        val executor = ApiRequestExecutor.Default(
            connectionFactory = FakeConnectionFactory(
                FailingConnection(
                    UnknownHostException("Could not connect to Stripe API")
                )
            )
        )

        val failure = assertFailsWith<APIConnectionException> {
            executor.executeInternal(FakeStripeRequest())
        }
        assertThat(failure.message)
            .isEqualTo("IOException during API request to Stripe (https://api.stripe.com): Could not connect to Stripe API. Please check your internet connection and try again. If this problem persists, you should check Stripe's service status at https://twitter.com/stripestatus, or let us know at support@stripe.com.")
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
        private val stripeConnection: StripeConnection
    ) : ConnectionFactory {
        override fun create(request: StripeRequest): StripeConnection {
            return stripeConnection
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
}
