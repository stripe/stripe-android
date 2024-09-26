package com.stripe.android.customersheet.data

import kotlin.time.Duration.Companion.seconds

internal data class CachedCustomerEphemeralKey(
    val customerId: String,
    val ephemeralKey: String,
    private val expiresAt: Int,
) {
    fun shouldRefresh(currentTimeInMillis: Long): Boolean {
        val remainingTime = expiresAt - (currentTimeInMillis / 1000)

        return remainingTime <= 5.seconds.inWholeSeconds
    }
}
