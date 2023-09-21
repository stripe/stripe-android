package com.stripe.android.core.strings

import android.content.Context
import androidx.annotation.RestrictTo

/**
 * A `ResolvableString` when implemented can resolve a string using an available Android context.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface ResolvableString {
    fun resolve(context: Context): String
}
