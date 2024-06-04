package com.stripe.android.core.strings.transformations

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface TransformOperation {
    fun transform(value: String): String
}
