package com.stripe.android.core.networking

const val TEST_HOST = "https://test.host.com"

const val TEST_RETRY_CODES_START = 401
const val TEST_RETRY_CODES_END = 456

// [HTTP_TOO_MANY_REQUESTS] is included in this range
private val TEST_RETRY_CODES: Iterable<Int> = TEST_RETRY_CODES_START..TEST_RETRY_CODES_END

internal class FakeStripeRequest(
    override val url: String = TEST_HOST,
    override val shouldCache: Boolean = false,
    override val method: Method = Method.POST,
    override val mimeType: MimeType = MimeType.Form,
    override val headers: Map<String, String> = emptyMap(),
    override val retryResponseCodes: Iterable<Int> = TEST_RETRY_CODES
) : StripeRequest()
