package com.stripe.android.paymentelement

/**
 * This feature allows initializing PaymentSheet/FlowController/EmbeddedPaymentElement
 * with a Checkout Session client secret instead of a PaymentIntent or SetupIntent.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Checkout session support is under construction and may change without notice."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS
)
annotation class CheckoutSessionPreview
