package com.stripe.android

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject

internal interface FingerprintDataStore {
    fun get(): LiveData<FingerprintData>
    fun save(fingerprintData: FingerprintData)

    class Default(context: Context) : FingerprintDataStore {
        private val prefs = context.getSharedPreferences(
            PREF_FILE, Context.MODE_PRIVATE
        )

        override fun get(): LiveData<FingerprintData> {
            return MutableLiveData(
                runCatching {
                    FingerprintData.fromJson(
                        JSONObject(prefs.getString(KEY_DATA, null).orEmpty())
                    )
                }.getOrDefault(FingerprintData())
            )
        }

        override fun save(fingerprintData: FingerprintData) {
            prefs.edit()
                .putString(KEY_DATA, fingerprintData.toJson().toString())
                .apply()
        }

        private companion object {
            private const val PREF_FILE = "FingerprintDataRepository"
            private const val KEY_DATA = "key_fingerprint_data"
        }
    }
}
