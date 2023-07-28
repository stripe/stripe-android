package com.stripe.android.core.networking

import androidx.work.Data
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object WorkManagerHelpers {

    private var networkClient: StripeNetworkClient? = null

    fun setNetworkClient(networkClient: StripeNetworkClient) {
        this.networkClient = networkClient
    }

    @Synchronized
    fun getOrCreateNetworkClient(): StripeNetworkClient {
        if (networkClient == null) {
            synchronized(WorkManagerHelpers::class) {
                if (networkClient == null) {
                    networkClient = DefaultStripeNetworkClient(
                        logger = Logger.getInstance(BuildConfig.DEBUG),
                    )
                }
            }
        }

        return networkClient!!
    }
}

internal inline fun <reified T> Data.getSerializable(key: String): T? {
    val json = getString(key) ?: return null
    return runCatching {
        Json.decodeFromString<T>(json)
    }.getOrNull()
}

internal inline fun <reified T> Data.Builder.putSerializable(
    key: String,
    value: T,
): Data.Builder {
    val json = runCatching { Json.encodeToString(value) }.getOrNull()
    return putString(key, json)
}
