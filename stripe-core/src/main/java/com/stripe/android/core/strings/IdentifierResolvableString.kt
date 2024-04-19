package com.stripe.android.core.strings

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.core.strings.transformations.TransformOperation

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class IdentifierResolvableString(
    @StringRes private val id: Int,
    private val args: List<Any?> = emptyList(),
    private val transformations: List<TransformOperation> = emptyList()
) : ResolvableString {
    @Suppress("SpreadOperator")
    override fun resolve(context: Context): String {
        return transformations.fold(
            initial = context.getString(id, *resolveArgs(context, args))
        ) { currentValue, transformation ->
            transformation.transform(currentValue)
        }
    }
}
