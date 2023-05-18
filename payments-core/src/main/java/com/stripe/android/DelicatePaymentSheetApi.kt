package com.stripe.android

import androidx.annotation.RestrictTo

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is a delicate API that is only intended for advanced users and not required " +
        "for most integrations.",
)
@Retention(AnnotationRetention.BINARY)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class DelicatePaymentSheetApi
