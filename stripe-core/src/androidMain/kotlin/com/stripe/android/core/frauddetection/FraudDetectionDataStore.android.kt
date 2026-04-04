package com.stripe.android.core.frauddetection

import android.content.Context
import androidx.annotation.RestrictTo
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun createFraudDetectionDataStore(
    context: Context,
    workContext: CoroutineContext = Dispatchers.IO,
): FraudDetectionDataStore {
    return DefaultFraudDetectionDataStore(
        backend = SharedPreferencesFraudDetectionDataStoreBackend(context.applicationContext),
        workContext = workContext,
    )
}

private class SharedPreferencesFraudDetectionDataStoreBackend(
    context: Context
) : FraudDetectionDataStoreBackend {
    private val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    override fun get(): String? {
        return prefs.getString(KEY_DATA, null)
    }

    override fun save(value: String) {
        prefs.edit()
            .putString(KEY_DATA, value)
            .apply()
    }

    private companion object {
        private const val PREF_FILE = "FraudDetectionDataStore"
        private const val KEY_DATA = "key_fraud_detection_data"
    }
}
