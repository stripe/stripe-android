package com.stripe.android

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Calendar
import org.json.JSONException
import org.json.JSONObject

internal interface FingerprintDataStore {
    fun get(): LiveData<FingerprintData?>
    fun save(fingerprintData: FingerprintData)

    class Default(context: Context) : FingerprintDataStore {
        private val prefs = context.getSharedPreferences(
            PREF_FILE, Context.MODE_PRIVATE
        )
        private val timestampSupplier: () -> Long = {
            Calendar.getInstance().timeInMillis
        }

        override fun get(): LiveData<FingerprintData?> {
            val fingerprintData = try {
                FingerprintData.fromJson(
                    JSONObject(prefs.getString(KEY_DATA, null).orEmpty())
                ).takeUnless { it.isExpired(timestampSupplier()) }
            } catch (e: JSONException) {
                null
            }

            return MutableLiveData<FingerprintData?>(fingerprintData)
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
