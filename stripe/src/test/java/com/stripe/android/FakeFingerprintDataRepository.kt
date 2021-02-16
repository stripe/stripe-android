package com.stripe.android

import java.util.UUID

internal class FakeFingerprintDataRepository(
    private val guid: UUID = UUID.randomUUID(),
    private val muid: UUID = UUID.randomUUID(),
    private val sid: UUID = UUID.randomUUID()
) : FingerprintDataRepository {
    override fun refresh() {
    }

    override fun get(): FingerprintData? {
        return FingerprintData(
            guid = guid.toString(),
            muid = muid.toString(),
            sid = sid.toString()
        )
    }

    override fun save(fingerprintData: FingerprintData) {
    }
}
