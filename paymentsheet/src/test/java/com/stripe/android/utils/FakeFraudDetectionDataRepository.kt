package com.stripe.android.utils

import app.cash.turbine.Turbine
import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository

internal class FakeFraudDetectionDataRepository : FraudDetectionDataRepository {
    val refreshCalls = Turbine<Pair<String, String?>>()

    override fun refresh(publishableKey: String, stripeAccountId: String?) {
        refreshCalls.add(publishableKey to stripeAccountId)
    }

    override suspend fun getCached(publishableKey: String, stripeAccountId: String?): FraudDetectionData? = null

    override suspend fun getLatest(publishableKey: String, stripeAccountId: String?): FraudDetectionData? {
        error("Awaited collection is not expected when sharing payment details")
    }

    override fun save(publishableKey: String, stripeAccountId: String?, fraudDetectionData: FraudDetectionData) {
        error("Saving fraud data is not expected when sharing payment details")
    }
}
