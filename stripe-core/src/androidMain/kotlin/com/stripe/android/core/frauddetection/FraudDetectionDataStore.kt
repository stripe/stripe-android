package com.stripe.android.core.frauddetection

import android.content.Context
import androidx.annotation.RestrictTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FraudDetectionDataStore {
    suspend fun get(): FraudDetectionData?
    fun save(fraudDetectionData: FraudDetectionData)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultFraudDetectionDataStore(
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
        prefs.edit()
            .putString(KEY_DATA, fraudDetectionData.toJson().toString())
            .apply()
    }

    private companion object {
        private const val PREF_FILE = "FraudDetectionDataStore"
        private const val KEY_DATA = "key_fraud_detection_data"
    }
}
