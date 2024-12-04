package com.stripe.android.customersheet.data

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal data class CachedCustomerEphemeralKey(
    val customerId: String,
    val ephemeralKey: String,
    private val expiresAt: Int,
) {
    fun shouldRefresh(currentTimeInMillis: Long): Boolean {
        val remainingTime = expiresAt - currentTimeInMillis.milliseconds.inWholeSeconds
        return remainingTime <= 5.minutes.inWholeSeconds
    }
}
