package com.stripe.android.core.strings

import android.content.Context
import androidx.annotation.StringRes

internal data class IdentifierResolvableString(
    @StringRes private val id: Int,
    private val args: List<Any?>,
    private val transform: (value: String) -> String = { it }
) : ResolvableString {
    @Suppress("SpreadOperator")
    override fun resolve(context: Context): String {
        return transform(context.getString(id, *resolveArgs(context, args)))
    }
}
