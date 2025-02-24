package com.stripe.android.utils

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class ForceNativeBankFlowTestRule(
    private val context: Context,
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        forceNativeBankAuth(true)
    }

    override fun finished(description: Description) {
        forceNativeBankAuth(false)
        super.finished(description)
    }

    private fun forceNativeBankAuth(force: Boolean) {
        val sharedPrefs = context.getSharedPreferences(
            "FINANCIAL_CONNECTIONS_DEBUG",
            Context.MODE_PRIVATE
        )

        val settings = sharedPrefs.getString("json", null)
        val settingsJson = settings?.let { Json.decodeFromString(JsonObject.serializer(), it) } ?: JsonObject(emptyMap())
        val newSettings = settingsJson.toMutableMap().apply {
            if (force) {
                put("financial_connections_override_native", JsonPrimitive("native"))
            } else {
                remove("financial_connections_override_native")
            }
        }

        sharedPrefs.edit {
            putString("json", JsonObject(newSettings).toString())
        }
    }
}
