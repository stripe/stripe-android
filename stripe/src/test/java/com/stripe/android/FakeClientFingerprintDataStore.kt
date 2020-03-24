package com.stripe.android

import java.util.UUID

internal class FakeClientFingerprintDataStore(
    private val muid: UUID = UUID.randomUUID(),
    private val sid: UUID = UUID.randomUUID()
) : ClientFingerprintDataStore {
    override fun getMuid(): String = muid.toString()
    override fun getSid(): String = sid.toString()
}
