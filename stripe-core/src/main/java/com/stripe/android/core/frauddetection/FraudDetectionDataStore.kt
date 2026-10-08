package com.stripe.android.core.frauddetection

import android.content.Context
import androidx.annotation.RestrictTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FraudDetectionDataStore {
    suspend fun get(publishableKey: String, stripeAccountId: String?): FraudDetectionData?
    fun save(publishableKey: String, stripeAccountId: String?, fraudDetectionData: FraudDetectionData)
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

    override suspend fun get(publishableKey: String, stripeAccountId: String?) = withContext(workContext) {
        runCatching {
            val json = JSONObject(prefs.getString(storageKey(publishableKey, stripeAccountId), null).orEmpty())
            val timestampSupplier = {
                json.optLong(FraudDetectionData.KEY_TIMESTAMP, -1)
            }
            FraudDetectionDataJsonParser(timestampSupplier).parse(json)
        }.getOrNull()
    }

    override fun save(publishableKey: String, stripeAccountId: String?, fraudDetectionData: FraudDetectionData) {
        prefs.edit()
            .putString(storageKey(publishableKey, stripeAccountId), fraudDetectionData.toJson().toString())
            .apply()
    }

    // The legacy unscoped entry cannot be attributed to credentials and must not be reused.
    private fun storageKey(publishableKey: String, stripeAccountId: String?): String {
        val credentials = JSONArray().put(publishableKey).put(stripeAccountId ?: JSONObject.NULL).toString()
        val hash = MessageDigest.getInstance("SHA-256").digest(credentials.toByteArray(Charsets.UTF_8))
        return "credentials_v1_" + hash.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        private const val PREF_FILE = "FraudDetectionDataStore"
    }
}
