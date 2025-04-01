package com.stripe.android.core.networking

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

private const val AnalyticsRequestV2StorageName = "StripeAnalyticsRequestV2Storage"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AnalyticsRequestV2Storage {
    suspend fun store(request: AnalyticsRequestV2): String
    suspend fun retrieve(id: String): AnalyticsRequestV2?
    suspend fun delete(id: String)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RealAnalyticsRequestV2Storage private constructor(
    private val sharedPrefs: SharedPreferences,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AnalyticsRequestV2Storage {

    @Inject constructor(context: Context) : this(
        sharedPrefs = context.getSharedPreferences(
            AnalyticsRequestV2StorageName,
            Context.MODE_PRIVATE,
        ),
    )

    override suspend fun store(request: AnalyticsRequestV2): String = withContext(dispatcher) {
        val id = UUID.randomUUID().toString()
        val encodedRequest = Json.encodeToString(request)
        sharedPrefs
            .edit()
            .putString(id, encodedRequest)
            .apply()
        id
    }

    override suspend fun retrieve(id: String): AnalyticsRequestV2? = withContext(dispatcher) {
        val encodedRequest = sharedPrefs.getString(id, null) ?: return@withContext null
        sharedPrefs.edit().remove(id).apply()

        runCatching<AnalyticsRequestV2> {
            Json.decodeFromString(encodedRequest)
        }.getOrNull()
    }

    override suspend fun delete(id: String) = withContext(dispatcher) {
        sharedPrefs.edit().remove(id).apply()
    }
}
