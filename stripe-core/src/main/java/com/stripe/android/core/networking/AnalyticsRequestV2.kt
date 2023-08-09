package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.HEADER_ORIGIN
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_CLIENT_ID
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_CREATED
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_EVENT_ID
import com.stripe.android.core.networking.AnalyticsRequestV2.Companion.PARAM_EVENT_NAME
import com.stripe.android.core.networking.StripeRequest.MimeType
import com.stripe.android.core.version.StripeSdkVersion.VERSION_NAME
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit.SECONDS

/**
 * Analytics request sent to r.stripe.com, which is the preferred service for analytics.
 * This is a POST request with [MimeType.Form] ContentType.
 *
 * It sets two headers required by r.stripe.com -
 *   [HEADER_ORIGIN] - Set from analytics server
 *   [HEADER_USER_AGENT] - Used for parsing client info, needs to conform the format starting with "Stripe/v1"
 *
 * It sets four params required r.stripe.com -
 *   [PARAM_CLIENT_ID] - A string identifying the client making the request, set from analytics server
 *   [PARAM_CREATED] - Timestamp when the event was created in seconds
 *   [PARAM_EVENT_NAME] - An identifying name for this type of event
 *   [PARAM_EVENT_ID] - UUID used to deduplicate events
 *
 * Additional params can be passed as constructor parameters.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AnalyticsRequestV2(
    @get:VisibleForTesting
    val eventName: String,
    private val clientId: String,
    origin: String,
    @get:VisibleForTesting
    val params: Map<String, *>
) : StripeRequest() {
    // Note: nested params are calculated as a json string, which is different from other requests
    // that uses form encoding.
    // E.g for a nested map with value {"key", {"nestedKey1" -> "value1", "nestedKey2" -> "value2"}}
    // The params are encoded as a prettified json format sorted by key as follows
    // key="{
    //   "nestedKey1": "value1",
    //   "nestedKey2": "value2"
    // }"
    // As opposed to
    // key[nestedKey1]="value1"&key[nestedKey2]="value2"
    @VisibleForTesting
    internal val postParameters: String = createParams(params + analyticParams())

    private val postBodyBytes: ByteArray
        @Throws(UnsupportedEncodingException::class)
        get() {
            return postParameters.toByteArray(Charsets.UTF_8)
        }

    private data class Parameter(
        private val key: String,
        private val value: String
    ) {
        override fun toString(): String {
            val encodedKey = urlEncode(key)
            val encodedValue = urlEncode(value)
            return "$encodedKey=$encodedValue"
        }

        @Throws(UnsupportedEncodingException::class)
        private fun urlEncode(str: String): String {
            // Preserve original behavior that passing null for an object id will lead
            // to us actually making a request to /v1/foo/null
            return URLEncoder.encode(str, Charsets.UTF_8.name())
        }
    }

    private fun createParams(map: Map<String, *>): String {
        val paramList = mutableListOf<Parameter>()
        QueryStringFactory.compactParams(map).forEach { (key, value) ->
            when (value) {
                is Map<*, *> -> {
                    paramList.add(Parameter(key, encodeMapParam(value)))
                }
                else -> {
                    paramList.add(Parameter(key, value.toString()))
                }
            }
        }
        return paramList.joinToString("&") {
            it.toString()
        }
    }

    private fun encodeMapParam(map: Map<*, *>, level: Int = 0): String {
        val stringBuilder = StringBuilder()
        var first = true
        stringBuilder.appendLine("{")
        map.toSortedMap { key1, key2 ->
            key1.toString().compareTo(key2.toString())
        }.forEach { (key, value) ->
            val encodedValue =
                when (value) {
                    is Map<*, *> -> {
                        encodeMapParam(value, level + 1)
                    }
                    null -> ""
                    else -> "\"$value\""
                }
            if (encodedValue.isNotBlank()) {
                if (first) {
                    stringBuilder.append(INDENTATION.repeat(level)).append("$INDENTATION\"$key\": $encodedValue")
                    first = false
                } else {
                    stringBuilder.appendLine(",").append(INDENTATION.repeat(level))
                        .append("$INDENTATION\"$key\": $encodedValue")
                }
            }
        }
        stringBuilder.appendLine()
        stringBuilder.append(INDENTATION.repeat(level)).append("}")
        return stringBuilder.toString()
    }

    /**
     * Parameters required by r.stripe.com
     */
    private fun analyticParams(): Map<String, Any> = mapOf(
        PARAM_CLIENT_ID to clientId,
        PARAM_CREATED to System.currentTimeMillis().milliseconds.toDouble(SECONDS),
        PARAM_EVENT_NAME to eventName,
        PARAM_EVENT_ID to UUID.randomUUID().toString()
    )

    override fun writePostBody(outputStream: OutputStream) {
        postBodyBytes.let {
            outputStream.write(it)
            outputStream.flush()
        }
    }

    override val headers = mapOf(
        HEADER_CONTENT_TYPE to "${MimeType.Form.code}; charset=${Charsets.UTF_8.name()}",
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

        internal const val PARAM_CLIENT_ID = "client_id"
        internal const val PARAM_CREATED = "created"
        internal const val PARAM_EVENT_NAME = "event_name"
        internal const val PARAM_EVENT_ID = "event_id"

        private const val INDENTATION = "  "
    }
}
