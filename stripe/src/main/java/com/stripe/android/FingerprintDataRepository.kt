package com.stripe.android

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONException
import org.json.JSONObject

internal interface FingerprintDataRepository {

    fun get(): LiveData<FingerprintData?>
    fun save(fingerprintData: FingerprintData)

    class Default(context: Context) : FingerprintDataRepository {
        private val prefs = context.getSharedPreferences(
            PREF_FILE, Context.MODE_PRIVATE
        )

        override fun get(): LiveData<FingerprintData?> {
            val resultData = MutableLiveData<FingerprintData?>()
            resultData.value = try {
                FingerprintData.fromJson(
                    JSONObject(prefs.getString(KEY, null).orEmpty())
                )
            } catch (e: JSONException) {
                null
            }

            return resultData
        }

        override fun save(fingerprintData: FingerprintData) {
            prefs.edit()
                .putString(KEY, fingerprintData.toJson().toString())
                .apply()
        }

        private companion object {
            private const val PREF_FILE = "FingerprintDataRepository"
            private const val KEY = "key"
        }
    }
}
