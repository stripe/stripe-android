package com.stripe.android.core.strings

import android.content.Context

internal data class StaticResolvableString(
    private val value: String,
    private val args: List<Any?>,
) : ResolvableString {
    @Suppress("SpreadOperator")
    override fun resolve(context: Context): String {
        return value.format(*resolveArgs(context, args))
    }
}
