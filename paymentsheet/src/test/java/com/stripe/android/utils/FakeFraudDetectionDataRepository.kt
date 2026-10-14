package com.stripe.android.utils

import app.cash.turbine.Turbine
import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository

internal class FakeFraudDetectionDataRepository : FraudDetectionDataRepository {
    val refreshCalls = Turbine<String>()

    override fun refresh(publishableKey: String) {
        refreshCalls.add(publishableKey)
    }

    override suspend fun getCached(): FraudDetectionData? = null

    override suspend fun getLatest(publishableKey: String): FraudDetectionData? {
        error("Awaited collection is not expected when sharing payment details")
    }

    override fun save(fraudDetectionData: FraudDetectionData) {
        error("Saving fraud data is not expected when sharing payment details")
    }
}
