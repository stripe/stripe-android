package com.stripe.android.core.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface UserFacingLogger {
    fun logWarningWithoutPii(message: String)
}
