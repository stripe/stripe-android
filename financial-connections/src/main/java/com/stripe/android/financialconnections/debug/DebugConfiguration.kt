package com.stripe.android.financialconnections.debug

import android.app.Application
import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

internal class DebugConfiguration @Inject constructor(
    context: Application
) {

    private val sharedPreferences = context
        .getSharedPreferences("FINANCIAL_CONNECTIONS_DEBUG", Context.MODE_PRIVATE)

    internal val overriddenNative: Boolean?
        get() = runCatching {
            sharedPreferences.getString("json", null)?.let {
                val jsonObject = Json.decodeFromString(JsonObject.serializer(), it)
                when (jsonObject[KEY_OVERRIDE_NATIVE]?.jsonPrimitive?.contentOrNull) {
                    "native" -> true
                    "web" -> false
                    else -> null
                }
            }
        }.getOrNull()
}

private const val KEY_OVERRIDE_NATIVE = "financial_connections_override_native"
