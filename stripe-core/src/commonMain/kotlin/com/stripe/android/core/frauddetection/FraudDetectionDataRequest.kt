package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.DEFAULT_RETRY_CODES
import com.stripe.android.core.networking.RequestHeadersFactory
import com.stripe.android.core.networking.StripeRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okio.BufferedSink

/**
 * A class representing a [FraudDetectionData] request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FraudDetectionDataRequest(
    private val params: Map<String, Any>,
    guid: String
) : StripeRequest() {
    private val jsonBody: String
        get() = params.toJsonObject().toString()

    private val headersFactory = RequestHeadersFactory.FraudDetection(
        guid = guid
    )

    override val method = Method.POST

    override val mimeType = MimeType.Json

    override val retryResponseCodes: Iterable<Int> = DEFAULT_RETRY_CODES

    override val url = URL

    override val headers = headersFactory.create()

    override var postHeaders: Map<String, String>? = headersFactory.createPostHeader()

    override fun writePostBody(sink: BufferedSink) {
        sink.write(jsonBody.encodeToByteArray())
        sink.flush()
    }

    private companion object {
        private const val URL = "https://m.stripe.com/6"
    }
}

private fun Map<*, *>.toJsonObject(): JsonObject {
    val map = mutableMapOf<String, JsonElement>()
    forEach { (key, value) ->
        val jsonKey = key as? String ?: return@forEach
        val jsonValue = value?.toJsonElement() ?: return@forEach
        map[jsonKey] = jsonValue
    }
    return JsonObject(map)
}

private fun List<*>.toJsonArray(): JsonArray {
    return JsonArray(map { value -> value.toJsonElement() })
}

private fun Any?.toJsonElement(): JsonElement {
    return when (this) {
        is Map<*, *> -> toJsonObject()
        is List<*> -> toJsonArray()
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        null -> JsonPrimitive("null")
        else -> JsonPrimitive(toString())
    }
}
