package com.stripe.android.core.strings.transformations

import android.os.Parcelable
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface TransformOperation : Parcelable {
    fun transform(value: String): String
}
