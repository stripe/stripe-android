package com.stripe.example.paymentsheet

import com.stripe.example.service.CheckoutResponse
import kotlinx.coroutines.flow.Flow

internal interface Repository {
    fun clearKeys()

    suspend fun fetchLocalEphemeralKey(): Flow<EphemeralKey?>

    suspend fun fetchRemoteEphemeralKey(): Flow<Result<EphemeralKey>>

    suspend fun checkout(
        customer: String,
        currency: String,
        mode: String
    ): Flow<Result<CheckoutResponse>>
}
