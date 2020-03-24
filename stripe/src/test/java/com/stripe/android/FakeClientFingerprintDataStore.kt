package com.stripe.android

import java.util.UUID

internal class FakeClientFingerprintDataStore(
    private val muid: UUID = UUID.randomUUID()
) : ClientFingerprintDataStore {
    override fun getMuid(): String = muid.toString()
}
