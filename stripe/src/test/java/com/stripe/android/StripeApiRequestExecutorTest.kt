package com.stripe.android

import com.stripe.android.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.function.ThrowingRunnable
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
        assertThrows<InvalidRequestException>(InvalidRequestException::class.java,
            ThrowingRunnable { connectionFactory.getRequestOutputBytes(stripeRequest) })
    }
}
