package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.networking.DEFAULT_RETRY_CODES
import com.stripe.android.core.networking.RequestHeadersFactory
import com.stripe.android.core.networking.StripeRequest
import java.io.OutputStream
import java.io.UnsupportedEncodingException

/**
 * A class representing a [FraudDetectionData] request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FraudDetectionDataRequest(
    private val params: Map<String, Any>,
    guid: String
) : StripeRequest() {
    private val jsonBody: String
        get() {
            return StripeJsonUtils.mapToJsonObject(params).toString()
        }
    private val postBodyBytes: ByteArray
        get() {
            try {
                return jsonBody.toByteArray(Charsets.UTF_8)
            } catch (e: UnsupportedEncodingException) {
                throw InvalidRequestException(
                    message = "Unable to encode parameters to ${Charsets.UTF_8.name()}. " +
                        "Please contact support@stripe.com for assistance.",
                    cause = e
                )
            }
        }

    private val headersFactory = RequestHeadersFactory.FraudDetection(
        guid = guid
    )

    override val method = Method.POST

    override val mimeType = MimeType.Json

    override val retryResponseCodes: Iterable<Int> = DEFAULT_RETRY_CODES

    override val url = URL

    override val headers = headersFactory.create()

    override var postHeaders: Map<String, String>? = headersFactory.createPostHeader()

    override fun writePostBody(outputStream: OutputStream) {
        postBodyBytes.let {
            outputStream.write(it)
            outputStream.flush()
        }
    }

    private companion object {
        private const val URL = "https://m.stripe.com/6"
    }
}
