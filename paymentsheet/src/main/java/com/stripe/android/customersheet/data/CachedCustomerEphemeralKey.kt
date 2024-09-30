package com.stripe.android.customersheet.data

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal sealed interface CachedCustomerEphemeralKey {
    fun shouldRefresh(currentTimeInMillis: Long): Boolean

    data class Available(
        val customerId: String,
        val ephemeralKey: String,
        private val expiresAt: Int,
    ) : CachedCustomerEphemeralKey {
        override fun shouldRefresh(currentTimeInMillis: Long): Boolean {
            val remainingTime = expiresAt - currentTimeInMillis.milliseconds.inWholeSeconds
            return remainingTime <= 5.minutes.inWholeSeconds
        }
    }

    data object None : CachedCustomerEphemeralKey {
        override fun shouldRefresh(currentTimeInMillis: Long): Boolean {
            return true
        }
    }
}

internal fun <T> Result<CachedCustomerEphemeralKey>.mapWithEphemeralKeyCatching(
    mapper: (customerId: String, ephemeralKey: String) -> T
): Result<T> {
    return mapCatching { cachedKey ->
        when (cachedKey) {
            is CachedCustomerEphemeralKey.Available -> mapper(cachedKey.customerId, cachedKey.ephemeralKey)
            is CachedCustomerEphemeralKey.None -> throw IllegalStateException("No ephemeral key was available!")
        }
    }
}
