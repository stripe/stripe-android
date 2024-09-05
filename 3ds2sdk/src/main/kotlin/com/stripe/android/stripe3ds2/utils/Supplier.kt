package com.stripe.android.stripe3ds2.utils

internal fun interface Supplier<SuppliedType> {
    fun get(): SuppliedType
}
