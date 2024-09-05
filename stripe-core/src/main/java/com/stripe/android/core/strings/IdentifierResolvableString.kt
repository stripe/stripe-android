package com.stripe.android.core.strings

import android.content.Context
import androidx.annotation.StringRes
import com.stripe.android.core.strings.transformations.TransformOperation
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
internal data class IdentifierResolvableString(
    @StringRes private val id: Int,
    private val transformations: List<TransformOperation> = listOf(),
    private val args: List<@RawValue Any?>,
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
