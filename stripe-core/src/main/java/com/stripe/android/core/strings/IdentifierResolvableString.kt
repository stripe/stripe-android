package com.stripe.android.core.strings

import android.content.Context
import android.icu.text.MessageFormat
import android.os.Build
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
            initial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                MessageFormat.format(context.getString(id), *resolveArgs(context, args))
            } else {
                context.getString(id, *resolveArgs(context, args))
            }
        ) { currentValue, transformation ->
            transformation.transform(currentValue)
        }
    }
}
