package com.stripe.android

import java.util.UUID

internal class FakeFingerprintDataRepository(
    private val guid: UUID = UUID.randomUUID()
) : FingerprintDataRepository {
    override fun refresh() {
    }

    override fun get(): FingerprintData? {
        return FingerprintData(
            guid = guid.toString()
        )
    }

    override fun save(fingerprintData: FingerprintData) {
    }
}
