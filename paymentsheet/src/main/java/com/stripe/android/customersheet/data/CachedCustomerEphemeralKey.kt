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
