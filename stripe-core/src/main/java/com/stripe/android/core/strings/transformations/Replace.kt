package com.stripe.android.core.strings.transformations

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class Replace(
    val original: String,
    val replacement: String
) : TransformOperation {
    override fun transform(value: String): String {
        return value.replace(original, replacement)
    }
}
