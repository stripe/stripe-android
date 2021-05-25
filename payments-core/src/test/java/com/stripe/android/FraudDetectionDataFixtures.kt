package com.stripe.android

import com.stripe.android.networking.FraudDetectionData
import java.util.UUID

internal object FraudDetectionDataFixtures {
    val DEFAULT = create()

    fun create(timestamp: Long = -1): FraudDetectionData {
        return FraudDetectionData(
            guid = UUID.randomUUID().toString(),
            muid = UUID.randomUUID().toString(),
            sid = UUID.randomUUID().toString(),
            timestamp = timestamp
        )
    }
}
