package com.stripe.android

internal interface SystemPropertySupplier {
    fun get(name: String): String
}
