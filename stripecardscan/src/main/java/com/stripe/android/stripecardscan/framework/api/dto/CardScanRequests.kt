package com.stripe.android.stripecardscan.framework.api.dto

import com.stripe.android.core.networking.HEADER_AUTHORIZATION
import com.stripe.android.core.networking.HEADER_CONTENT_TYPE
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.stripecardscan.framework.api.CARD_SCAN_RETRY_STATUS_CODES
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * A class representing a StripeCardScan's API request.
 */
internal data class CardScanRequest internal constructor(
    override val method: Method,
    override val retryResponseCodes: Iterable<Int>,
    internal val baseUrl: String,
    internal val path: String,
    internal val stripePublishableKey: String,
    internal val encodedPostData: String? = null
) : StripeRequest() {
    override val mimeType: MimeType = MimeType.Form

    override val url: String
        get() {
            val fullPath = if (path.startsWith("/")) path else "/$path"
            return "$baseUrl$fullPath"
        }

    override val headers = mapOf(
        HEADER_AUTHORIZATION to "Bearer $stripePublishableKey"
    )

    override var postHeaders: Map<String, String>? = mapOf(
        HEADER_CONTENT_TYPE to MimeType.Form.toString()
    )

    override fun writePostBody(outputStream: OutputStream) {
        OutputStreamWriter(outputStream).use {
            it.write(encodedPostData)
            it.flush()
        }
    }

    override fun toString(): String {
        return "${method.code} $url"
    }

    internal companion object {
        fun createGet(
            stripePublishableKey: String,
            baseUrl: String,
            path: String,
            retryResponseCodes: Iterable<Int>
        ): CardScanRequest {
            return CardScanRequest(
                method = Method.GET,
                retryResponseCodes = retryResponseCodes,
                baseUrl = baseUrl,
                path = path,
                stripePublishableKey = stripePublishableKey
            )
        }

        fun createPost(
            stripePublishableKey: String,
            baseUrl: String,
            path: String,
            encodedPostData: String,
            retryResponseCodes: Iterable<Int>
        ): CardScanRequest {
            return CardScanRequest(
                method = Method.POST,
                retryResponseCodes = retryResponseCodes,
                baseUrl = baseUrl,
                path = path,
                stripePublishableKey = stripePublishableKey,
                encodedPostData = encodedPostData
            )
        }
    }
}

/**
 * A class representing a StripeCardScan's request to download a file.
 */
internal data class CardScanFileDownloadRequest(
    override val url: String
) : StripeRequest() {
    override val method: Method = Method.GET
    override val mimeType: MimeType = MimeType.Form
    override val retryResponseCodes = CARD_SCAN_RETRY_STATUS_CODES
    override val headers: Map<String, String> = mapOf()
}
