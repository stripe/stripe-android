package com.stripe.android.core.strings

import android.content.Context
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
internal data class StaticResolvableString(
    private val value: String,
    private val args: List<@RawValue Any?>,
) : ResolvableString {
    @Suppress("SpreadOperator")
    override fun resolve(context: Context): String {
        return value.format(*resolveArgs(context, args))
    }
}
