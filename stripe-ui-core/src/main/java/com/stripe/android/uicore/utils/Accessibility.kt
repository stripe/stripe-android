package com.stripe.android.uicore.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun String.asIndividualDigits(): String {
    return toCharArray().joinToString(separator = " ")
}
