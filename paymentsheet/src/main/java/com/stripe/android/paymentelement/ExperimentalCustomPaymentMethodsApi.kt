package com.stripe.android.paymentelement

import androidx.annotation.RestrictTo

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Custom payment methods support is beta. It may be changed in the future without notice."
)
@Retention(AnnotationRetention.BINARY)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class ExperimentalCustomPaymentMethodsApi
