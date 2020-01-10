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
    fun bodyBytes_shouldHandleUnsupportedEncodingException() {
        val stripeRequest = object : StripeRequest() {
            override val method: Method = Method.POST
            override val baseUrl: String = ApiRequest.API_HOST
            override val params: Map<String, *>? = null
            override val mimeType: MimeType = MimeType.Form
            override val userAgent: String = DEFAULT_USER_AGENT

            override val body: String
                get() {
                    throw UnsupportedEncodingException()
                }

            override fun createHeaders(): Map<String, String> {
                return emptyMap()
            }
        }

        assertFailsWith<InvalidRequestException> {
            stripeRequest.bodyBytes
        }
    }
}
