package com.stripe.android.core.strings.transformations

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class Append(
    private val original: String,
    private val appendix: String,
    private val separator: String,
) : TransformOperation {
    override fun transform(value: String): String {
        return original + separator + appendix
    }
}
