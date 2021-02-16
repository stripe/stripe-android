package com.stripe.example.paymentsheet

import kotlinx.coroutines.flow.Flow

internal interface Repository {
    fun clearKeys()

    suspend fun fetchLocalEphemeralKey(): Flow<EphemeralKey?>

    suspend fun fetchRemoteEphemeralKey(): Flow<Result<EphemeralKey>>
}
