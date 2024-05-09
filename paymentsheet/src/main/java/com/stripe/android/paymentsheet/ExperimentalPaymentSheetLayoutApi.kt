package com.stripe.android.paymentsheet

import androidx.annotation.RestrictTo

@RequiresOptIn(message = "Layout support is beta. It may be changed in the future without notice.")
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class ExperimentalPaymentSheetLayoutApi
