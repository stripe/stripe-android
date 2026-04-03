package com.stripe.android.core.strings

import android.content.Context
import android.os.Parcelable
import androidx.annotation.RestrictTo

/**
 * A `ResolvableString` when implemented can resolve a string using an available Android context.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ResolvableString : Parcelable {
    fun resolve(context: Context): String
}
