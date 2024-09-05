package com.stripe.android.core.strings.transformations

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class Replace(
    private val original: String,
    private val replacement: String
) : TransformOperation {
    override fun transform(value: String): String {
        return value.replace(original, replacement)
    }
}
