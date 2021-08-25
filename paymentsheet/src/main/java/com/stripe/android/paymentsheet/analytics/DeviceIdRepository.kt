package com.stripe.android.paymentsheet.analytics

internal interface DeviceIdRepository {
    suspend fun get(): DeviceId
}
