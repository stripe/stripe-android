package com.stripe.android

import java.util.UUID

internal class StripeOperationIdFactory : OperationIdFactory {
    override fun create(): String {
        return UUID.randomUUID().toString()
    }
}
