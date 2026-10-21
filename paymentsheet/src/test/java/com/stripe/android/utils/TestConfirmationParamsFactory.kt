package com.stripe.android.utils

import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.networking.ConfirmationParamsFactory

internal object TestConfirmationParamsFactory {
    private val DEFAULT_FRAUD_DETECTION_DATA = FraudDetectionData(
        guid = "test_guid",
        muid = "test_muid",
        sid = "test_sid",
    )

    fun create(): ConfirmationParamsFactory = ConfirmationParamsFactory(
        productUsageTokens = emptySet(),
        fraudDetectionDataRepository = FakeFraudDetectionDataRepository(DEFAULT_FRAUD_DETECTION_DATA),
    )
}

internal class FakeFraudDetectionDataRepository(
    private var cachedFraudDetectionData: FraudDetectionData?,
) : FraudDetectionDataRepository {
    override fun refresh() = Unit

    override fun getCached(): FraudDetectionData? = cachedFraudDetectionData

    override suspend fun getLatest(): FraudDetectionData? = cachedFraudDetectionData

    override fun save(fraudDetectionData: FraudDetectionData) {
        cachedFraudDetectionData = fraudDetectionData
    }
}
