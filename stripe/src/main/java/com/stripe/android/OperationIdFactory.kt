package com.stripe.android

import java.util.UUID

internal open class OperationIdFactory {
    open fun create(): String {
        return UUID.randomUUID().toString()
    }
}
