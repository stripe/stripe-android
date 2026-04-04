package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FraudDetectionDataRepository {
    fun refresh()

    /**
     * Get the cached [FraudDetectionData]. This is not a blocking request.
     */
    fun getCached(): FraudDetectionData?

    /**
     * Get the latest [FraudDetectionData]. This is a blocking request.
     *
     * 1. From local storage if that value is not expired.
     * 2. Otherwise, from the network.
     */
    suspend fun getLatest(): FraudDetectionData?

    fun save(fraudDetectionData: FraudDetectionData)
}
