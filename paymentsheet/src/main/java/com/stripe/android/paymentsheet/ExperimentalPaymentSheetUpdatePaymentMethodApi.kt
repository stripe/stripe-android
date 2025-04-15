package com.stripe.android.paymentsheet

import androidx.annotation.RestrictTo

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Payment Method Update support is in beta. It may be changed in the future without notice.",
)
@Retention(AnnotationRetention.BINARY)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class ExperimentalPaymentSheetUpdatePaymentMethodApi
