package com.stripe.android

import com.stripe.android.exception.InvalidRequestException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
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
            override val headersFactory = RequestHeadersFactory.Default()

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
}
