package com.stripe.android

internal fun interface OperationIdFactory {
    fun create(): String

    companion object {
        @JvmSynthetic
        internal fun get(): OperationIdFactory = StripeOperationIdFactory()
    }
}
