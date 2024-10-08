package com.stripe.android.core.error

import com.stripe.android.core.networking.HTTP_TOO_MANY_REQUESTS
import com.stripe.android.core.networking.StripeRequest
import kotlinx.serialization.Serializable
import java.io.OutputStream

@Serializable
data class SentryErrorRequest(
    val envelopeBody: String,
    val projectId: String,
    override val headers: Map<String, String>,
) : StripeRequest() {

    private val postBodyBytes: ByteArray
        get() = envelopeBody.toByteArray(Charsets.UTF_8)

    override fun writePostBody(outputStream: OutputStream) {
        outputStream.write(postBodyBytes)
        outputStream.flush()
    }

    override val method: Method = Method.POST

    override val mimeType: MimeType = MimeType.Json

    override val url: String = "$HOST/api/$projectId/envelope/"

    // TODO@carlosmuvi Sentry responses return a delay period along with rate limit responses; handle it.
    override val retryResponseCodes: Iterable<Int> = HTTP_TOO_MANY_REQUESTS..HTTP_TOO_MANY_REQUESTS

    companion object {
        private const val HOST = "https://errors.stripe.com"
    }
}
