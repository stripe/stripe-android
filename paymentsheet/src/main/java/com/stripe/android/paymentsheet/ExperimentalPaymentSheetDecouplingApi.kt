package com.stripe.android.paymentsheet

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an experimental API for finalizing payments on the server when using " +
        "Stripe's PaymentSheet. It’s currently in private beta and is likely to change before " +
        "the public release. For details and early access, visit " +
        "https://stripe.com/docs/payments/finalize-payments-on-the-server?platform=android",
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalPaymentSheetDecouplingApi
