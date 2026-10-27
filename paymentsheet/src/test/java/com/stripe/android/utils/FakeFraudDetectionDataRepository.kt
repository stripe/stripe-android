package com.stripe.android.utils

import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository

internal class FakeFraudDetectionDataRepository private constructor(
    private var cachedFraudDetectionData: FraudDetectionData?,
) : FraudDetectionDataRepository {
    override fun refresh() = Unit

    override fun getCached(): FraudDetectionData? = cachedFraudDetectionData

    override suspend fun getLatest(): FraudDetectionData? = cachedFraudDetectionData

    override fun save(fraudDetectionData: FraudDetectionData) {
        cachedFraudDetectionData = fraudDetectionData
    }

    companion object {
        fun create(): FakeFraudDetectionDataRepository = FakeFraudDetectionDataRepository(
            cachedFraudDetectionData = FraudDetectionData(
                guid = "test_guid",
                muid = "test_muid",
                sid = "test_sid",
            )
        )
    }
}
