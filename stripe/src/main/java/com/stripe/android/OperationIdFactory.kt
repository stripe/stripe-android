package com.stripe.android

internal interface OperationIdFactory {
    fun create(): String

    companion object {
        @JvmSynthetic
        internal fun get(): OperationIdFactory = StripeOperationIdFactory()
    }
}
