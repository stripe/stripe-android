package com.stripe.android

import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import java.util.UUID

internal class FakeFraudDetectionDataRepository(
    private val fraudDetectionData: FraudDetectionData?
) : FraudDetectionDataRepository {

    @JvmOverloads constructor(
        guid: UUID = UUID.randomUUID(),
        muid: UUID = UUID.randomUUID(),
        sid: UUID = UUID.randomUUID()
    ) : this(
        FraudDetectionData(
            guid = guid.toString(),
            muid = muid.toString(),
            sid = sid.toString()
        )
    )

    override fun refresh(publishableKey: String, stripeAccountId: String?) {
    }

    override suspend fun getCached(publishableKey: String, stripeAccountId: String?) = fraudDetectionData

    override suspend fun getLatest(publishableKey: String, stripeAccountId: String?) = fraudDetectionData

    override fun save(publishableKey: String, stripeAccountId: String?, fraudDetectionData: FraudDetectionData) {
    }
}
