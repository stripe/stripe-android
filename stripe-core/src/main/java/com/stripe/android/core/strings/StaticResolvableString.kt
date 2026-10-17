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
        val resolved = resolveArgs(context, args)
        // Avoid running String.format when there are no args: the value may be raw user input
        // containing percent signs (e.g. "%@", "50%"), which would cause a
        // java.util.UnknownFormatConversionException if passed through String.format as a
        // format string.
        return if (resolved.isEmpty()) value else value.format(*resolved)
    }
}
