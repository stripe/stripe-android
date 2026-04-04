package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FraudDetectionDataStore {
    suspend fun get(): FraudDetectionData?
    fun save(fraudDetectionData: FraudDetectionData)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FraudDetectionDataStoreBackend {
    fun get(): String?
    fun save(value: String)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultFraudDetectionDataStore(
    private val backend: FraudDetectionDataStoreBackend,
    private val workContext: CoroutineContext,
    private val fraudDetectionDataJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : FraudDetectionDataStore {
    override suspend fun get() = withContext(workContext) {
        runCatching {
            backend.get()
                ?.takeIf { it.isNotBlank() }
                ?.let { storedData ->
                    fraudDetectionDataJson.decodeFromString<FraudDetectionData>(storedData)
                }
        }.getOrNull()
    }

    override fun save(fraudDetectionData: FraudDetectionData) {
        backend.save(
            fraudDetectionDataJson.encodeToString(fraudDetectionData)
        )
    }
}
