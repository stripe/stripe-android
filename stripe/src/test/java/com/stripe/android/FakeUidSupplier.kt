package com.stripe.android

import java.util.UUID

internal class FakeUidSupplier @JvmOverloads constructor(
    private val value: String = UUID.randomUUID().toString()
) : Supplier<StripeUid> {

    override fun get(): StripeUid {
        return StripeUid(value)
    }
}
