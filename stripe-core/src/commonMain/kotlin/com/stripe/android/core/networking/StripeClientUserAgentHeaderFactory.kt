package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.AppInfo
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StripeClientUserAgentHeaderFactory(
    private val systemPropertySupplier: (String) -> String = DEFAULT_SYSTEM_PROPERTY_SUPPLIER
) {
    fun create(
        appInfo: AppInfo? = null
    ): Map<String, String> {
        return mapOf(
            HEADER_STRIPE_CLIENT_USER_AGENT to createHeaderValue(appInfo).toString()
        )
    }

    @VisibleForTesting
    fun createHeaderValue(
        appInfo: AppInfo? = null
    ): JsonObject {
        val platformData = defaultRequestHeadersPlatformData()
        val values = linkedMapOf<String, JsonElement>(
            "os.name" to JsonPrimitive(platformData.osName),
            "os.version" to JsonPrimitive(platformData.osVersion),
            "bindings.version" to JsonPrimitive(StripeSdkVersion.VERSION_NAME),
            "lang" to JsonPrimitive("Java"),
            "publisher" to JsonPrimitive("Stripe"),
            "http.agent" to JsonPrimitive(systemPropertySupplier(PROP_USER_AGENT))
        )
        appInfo?.createClientHeaders()?.forEach { (key, value) ->
            values[key] = value.toJsonObject()
        }
        return JsonObject(values)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        // this is the default user agent set by the system
        private const val PROP_USER_AGENT = "http.agent"

        private val DEFAULT_SYSTEM_PROPERTY_SUPPLIER = { name: String ->
            defaultSystemProperty(name)
        }

        const val HEADER_STRIPE_CLIENT_USER_AGENT = "X-Stripe-Client-User-Agent"
    }
}

private fun Map<String, String?>.toJsonObject(): JsonObject {
    return JsonObject(
        mapValues { (_, value) ->
            value?.let(::JsonPrimitive) ?: JsonNull
        }
    )
}
