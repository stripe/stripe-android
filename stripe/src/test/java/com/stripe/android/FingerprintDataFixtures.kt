package com.stripe.android

import java.util.UUID

internal object FingerprintDataFixtures {
    val DEFAULT = create()

    fun create(timestamp: Long = -1): FingerprintData {
        return FingerprintData(
            guid = UUID.randomUUID().toString(),
            muid = UUID.randomUUID().toString(),
            sid = UUID.randomUUID().toString(),
            timestamp = timestamp
        )
    }
}
