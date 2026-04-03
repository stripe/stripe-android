package com.stripe.android.core.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface IsWorkManagerAvailable {
    suspend operator fun invoke(): Boolean
}
