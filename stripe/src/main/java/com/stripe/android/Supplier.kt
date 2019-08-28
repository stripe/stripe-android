package com.stripe.android

internal interface Supplier<ReturnType> {
    fun get(): ReturnType
}
