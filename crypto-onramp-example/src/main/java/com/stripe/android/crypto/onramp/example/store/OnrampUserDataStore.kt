package com.stripe.android.crypto.onramp.example.store

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.stripe.android.crypto.onramp.example.model.OnrampUserData
import kotlinx.serialization.json.Json

internal const val ONRAMP_PREFS_NAME = "onramp_prefs"

internal class OnrampUserDataStore(
    context: Context,
    private val prefs: SharedPreferences = context.getSharedPreferences(
        ONRAMP_PREFS_NAME,
        Context.MODE_PRIVATE
    ),
    private val json: Json = Json,
) {
    fun save(userData: OnrampUserData) {
        prefs.edit {
            putString(USER_DATA_KEY, json.encodeToString(OnrampUserData.serializer(), userData))
        }
    }

    fun load(): OnrampUserData? {
        val serialized = prefs.getString(USER_DATA_KEY, null) ?: return null
        return json.decodeFromString(OnrampUserData.serializer(), serialized)
    }

    fun clear() {
        prefs.edit { remove(USER_DATA_KEY) }
    }
}

private const val USER_DATA_KEY = "onramp_user_data"
