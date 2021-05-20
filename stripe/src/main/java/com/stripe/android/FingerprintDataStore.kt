package com.stripe.android

import android.content.Context
import androidx.core.content.edit
import com.stripe.android.model.parsers.FingerprintDataJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

internal interface FingerprintDataStore {
    suspend fun get(): FingerprintData?
    fun save(fingerprintData: FingerprintData)

    class Default(
        context: Context,
        private val workContext: CoroutineContext = Dispatchers.IO
    ) : FingerprintDataStore {
        private val prefs by lazy {
            context.getSharedPreferences(
                PREF_FILE,
                Context.MODE_PRIVATE
            )
        }

        override suspend fun get() = withContext(workContext) {
            runCatching {
                val json = JSONObject(prefs.getString(KEY_DATA, null).orEmpty())
                val timestampSupplier = {
                    json.optLong(FingerprintData.KEY_TIMESTAMP, -1)
                }
                FingerprintDataJsonParser(timestampSupplier).parse(json)
            }.getOrNull()
        }

        override fun save(fingerprintData: FingerprintData) {
            prefs.edit {
                putString(KEY_DATA, fingerprintData.toJson().toString())
            }
        }

        private companion object {
            private const val PREF_FILE = "FingerprintDataRepository"
            private const val KEY_DATA = "key_fingerprint_data"
        }
    }
}
