package com.stripe.android

import android.content.Context
import androidx.core.content.edit
import com.stripe.android.model.parsers.FraudDetectionDataJsonParser
import com.stripe.android.networking.FraudDetectionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

internal interface FraudDetectionDataStore {
    suspend fun get(): FraudDetectionData?
    fun save(fraudDetectionData: FraudDetectionData)
}

internal class DefaultFraudDetectionDataStore(
    context: Context,
    private val workContext: CoroutineContext = Dispatchers.IO
) : FraudDetectionDataStore {
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
                json.optLong(FraudDetectionData.KEY_TIMESTAMP, -1)
            }
            FraudDetectionDataJsonParser(timestampSupplier).parse(json)
        }.getOrNull()
    }

    override fun save(fraudDetectionData: FraudDetectionData) {
        prefs.edit {
            putString(KEY_DATA, fraudDetectionData.toJson().toString())
        }
    }

    private companion object {
        private const val PREF_FILE = "FraudDetectionDataStore"
        private const val KEY_DATA = "key_fraud_detection_data"
    }
}
