package com.stripe.android.networking

import androidx.annotation.RestrictTo
import java.io.OutputStream

/**
 * A class representing a request to a Stripe-owned service.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class StripeRequest {
    /**
     * The type(Get/Post/Delete) of this request.
     */
    internal abstract val method: Method

    /**
     * The MimeType of this request, determines the header and body format for a POST request
     */
    internal abstract val mimeType: MimeType

    /**
     * A range of HTTP response code indicating this request should be retried
     */
    abstract val retryResponseCodes: Iterable<Int>

    /**
     * The url of the request, could be concatenated with query params
     */
    abstract val url: String

    /**
     * The HTTP headers attached to this request
     */
    abstract val headers: Map<String, String>

    /**
     * Additional HTTP headers attached if this is a POST request
     */
    internal open var postHeaders: Map<String, String>? = null

    /**
     * Writes the body of a POST request with [OutputStream], left empty for non-POST requests
     */
    internal open fun writePostBody(outputStream: OutputStream) {}

    enum class Method(val code: String) {
        GET("GET"),
        POST("POST"),
        DELETE("DELETE")
    }

    enum class MimeType(val code: String) {
        Form("application/x-www-form-urlencoded"),
        MultipartForm("multipart/form-data"),
        Json("application/json");

        override fun toString(): String = code
    }
}
