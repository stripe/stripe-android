package com.stripe.android

internal class StripeSystemPropertySupplier : SystemPropertySupplier {
    override fun get(name: String): String {
        return System.getProperty(name).orEmpty()
    }
}
