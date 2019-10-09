package com.stripe.android

import java.util.UUID

internal class FakeSystemPropertySupplier(
    private val propVal: String = UUID.randomUUID().toString()
) : SystemPropertySupplier {
    override fun get(name: String): String {
        return propVal
    }
}
