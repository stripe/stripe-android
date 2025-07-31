package com.stripe.android.elements

@RequiresOptIn(message = "Customer session support is beta. It may be changed in the future without notice.")
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
annotation class CustomerSessionApiPreview
