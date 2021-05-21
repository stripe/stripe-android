package com.stripe.android

import java.util.UUID

internal class FakeFingerprintDataRepository(
    private val fingerprintData: FingerprintData?
) : FingerprintDataRepository {

    @JvmOverloads constructor(
        guid: UUID = UUID.randomUUID(),
        muid: UUID = UUID.randomUUID(),
        sid: UUID = UUID.randomUUID()
    ) : this(
        FingerprintData(
            guid = guid.toString(),
            muid = muid.toString(),
            sid = sid.toString()
        )
    )

    override fun refresh() {
    }

    override fun getCached() = fingerprintData

    override suspend fun getLatest() = fingerprintData

    override fun save(fingerprintData: FingerprintData) {
    }
}
