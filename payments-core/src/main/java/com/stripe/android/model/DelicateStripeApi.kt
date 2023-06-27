package com.stripe.android.model

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is a delicate API that is only intended for advanced users and not required " +
        "for most integrations.",
)
@Retention(AnnotationRetention.BINARY)
annotation class DelicateStripeApi
