package com.stripe.android

import com.stripe.android.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeApiRequestExecutorTest {
    @Test
    fun getOutputBytes_shouldHandleUnsupportedEncodingException() {
        val stripeRequest = object : StripeRequest(
            Method.POST,
            ApiRequest.API_HOST,
            null,
            ApiRequest.MIME_TYPE
        ) {

            override fun getUserAgent(): String {
                return DEFAULT_USER_AGENT
            }

            @Throws(UnsupportedEncodingException::class)
            override fun getOutputBytes(): ByteArray {
                throw UnsupportedEncodingException()
            }

            override fun createHeaders(): Map<String, String> {
                return emptyMap()
            }
        }

        val connectionFactory = ConnectionFactory()
        assertFailsWith<InvalidRequestException> {
            connectionFactory.getRequestOutputBytes(stripeRequest)
        }
    }
}
