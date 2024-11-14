package com.stripe.android.core.frauddetection

import java.util.UUID

internal object FraudDetectionDataFixtures {
    fun create(timestamp: Long = -1): FraudDetectionData {
        return FraudDetectionData(
            guid = UUID.randomUUID().toString(),
            muid = UUID.randomUUID().toString(),
            sid = UUID.randomUUID().toString(),
            timestamp = timestamp
        )
    }
}
