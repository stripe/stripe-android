package com.stripe.android.core.strings

import android.content.Context
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ConcatResolvableString(
    private val first: ResolvableString,
    private val second: ResolvableString,
) : ResolvableString {
    @Suppress("SpreadOperator")
    override fun resolve(context: Context): String {
        return first.resolve(context) + second.resolve(context)
    }
}
