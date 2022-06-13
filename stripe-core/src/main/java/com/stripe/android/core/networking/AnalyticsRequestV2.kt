package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.HEADER_ORIGIN
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_CREATED
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_EVENT_ID
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_EVENT_NAME
import com.stripe.android.core.networking.StripeRequest.MimeType
import com.stripe.android.core.version.StripeSdkVersion.VERSION_NAME
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.util.UUID

/**
 * Analytics request sent to r.stripe.com, which is the preferred service for analytics.
 * This is a POST request with [MimeType.Form] ContentType.
 *
 * It sets two headers required by r.stripe.com -
 *   [HEADER_ORIGIN] - Set from analytics server
 *   [HEADER_USER_AGENT] - Used for parsing client info, needs to conform the format starting with "Stripe/v1"
 *
 * It sets four params required r.stripe.com -
 *   [PARAM_EVENT_ID] - A string identifying the client making the request, set from analytics server
 *   [PARAM_CREATED] - Timestamp when the event was created in seconds
 *   [PARAM_EVENT_NAME] - An identifying name for this type of event
 *   [PARAM_EVENT_ID] - UUID used to deduplicate events
 *
 * Additional params can be passed as constructor parameters.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AnalyticsRequestV2(
    private val eventName: String,
    private val clientId: String,
    origin: String,
    params: Map<String, *>
) : StripeRequest() {
    @VisibleForTesting
    internal val postParameters: String =
        QueryStringFactory.createFromParamsWithEmptyValues(params + analyticParams())

    private val postBodyBytes: ByteArray
        @Throws(UnsupportedEncodingException::class)
        get() {
            return postParameters.toByteArray(Charsets.UTF_8)
        }

    /**
     * Parameters required by r.stripe.com
     */
    private fun analyticParams() = mapOf(
        PARAM_CLIENT_ID to clientId,
        PARAM_CREATED to System.currentTimeMillis() * MILLIS_IN_SECOND,
        PARAM_EVENT_NAME to eventName,
        PARAM_EVENT_ID to UUID.randomUUID().toString(),
    )

    override fun writePostBody(outputStream: OutputStream) {
        postBodyBytes.let {
            outputStream.write(it)
            outputStream.flush()
        }
    }

    override val headers = mapOf(
        HEADER_CONTENT_TYPE to "${MimeType.Form}; charset=${Charsets.UTF_8.name()}",
        HEADER_ORIGIN to origin, // required by r.stripe.com
        HEADER_USER_AGENT to "Stripe/v1 android/$VERSION_NAME" // required by r.stripe.com
    )
    override val method: Method = Method.POST

    override val mimeType: MimeType = MimeType.Form

    override val retryResponseCodes: Iterable<Int> = HTTP_TOO_MANY_REQUESTS..HTTP_TOO_MANY_REQUESTS

    override val url = ANALYTICS_HOST

    internal companion object {
        internal const val ANALYTICS_HOST = "https://r.stripe.com/0"
        internal const val HEADER_ORIGIN = "origin"
        internal const val MILLIS_IN_SECOND = 1000

        internal const val PARAM_CLIENT_ID = "client_id"
        internal const val PARAM_CREATED = "created"
        internal const val PARAM_EVENT_NAME = "event_name"
        internal const val PARAM_EVENT_ID = "event_id"
    }
}
