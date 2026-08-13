package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.HEADER_ORIGIN
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_CLIENT_ID
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_CREATED
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_EVENT_ID
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_EVENT_NAME
import java.io.OutputStream
import java.util.UUID

/**
 * Analytics request for the legacy Payment SDK analytics schema, POSTed to [HOST] (r.stripe.com) as
 * a form-encoded body.
 *
 * The legacy params are sent verbatim, alongside the four params r.stripe.com requires:
 * [PARAM_CLIENT_ID], [PARAM_CREATED], [PARAM_EVENT_NAME] and [PARAM_EVENT_ID]. [PARAM_EVENT_NAME]
 * and [PARAM_CREATED] duplicate the legacy `event` and `timestamp` params so that the legacy schema
 * keeps working.
 *
 * Analytics are saved in a shared DB table with a payment-specific schema. For other SDKs, prefer a
 * dedicated table via [AnalyticsRequestV2].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsRequest(
    val params: Map<String, *>,
    override val headers: Map<String, String>
) : StripeRequest() {
    /**
     * Computed eagerly rather than in [writePostBody], because a 429 retry re-invokes
     * [writePostBody] on the same instance and [PARAM_EVENT_ID] must be stable for dedup.
     */
    private val postBody: String =
        QueryStringFactory.createFromParamsWithEmptyValues(params + rStripeParams())

    override val method: Method = Method.POST

    override val mimeType: MimeType = MimeType.Form

    override val retryResponseCodes: Iterable<Int> = HTTP_TOO_MANY_REQUESTS..HTTP_TOO_MANY_REQUESTS

    override val url: String = HOST

    override var postHeaders: Map<String, String>? = mapOf(
        HEADER_CONTENT_TYPE to "${MimeType.Form.code}; charset=${Charsets.UTF_8.name()}",
        HEADER_ORIGIN to ORIGIN,
    )

    override fun writePostBody(outputStream: OutputStream) {
        outputStream.write(postBody.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    /**
     * Params required by r.stripe.com. [PARAM_EVENT_NAME] and [PARAM_CREATED] mirror the legacy
     * `event` and `timestamp` params, and are omitted when those are absent.
     */
    private fun rStripeParams(): Map<String, Any> = buildMap {
        put(PARAM_CLIENT_ID, CLIENT_ID)
        put(PARAM_EVENT_ID, UUID.randomUUID().toString())
        params[AnalyticsFields.EVENT]?.let { put(PARAM_EVENT_NAME, it) }
        params[AnalyticsFields.TIMESTAMP]?.let { put(PARAM_CREATED, it) }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val HOST = "https://r.stripe.com/0"

        private const val CLIENT_ID = "stripe-mobile-payments-sdk"
        private const val ORIGIN = "stripe-mobile-payments-sdk-android"
    }
}
