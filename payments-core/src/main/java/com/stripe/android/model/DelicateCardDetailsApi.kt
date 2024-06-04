package com.stripe.android.model

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is a delicate API that exposes sensitive card details. " +
        "Only do this if you're certain that you fulfill the necessary PCI compliance requirements. " +
        "Make sure that you're not mistakenly logging or storing full card details. " +
        "See the docs for details: https://stripe.com/docs/security/guide#validating-pci-compliance",
)
@Retention(AnnotationRetention.BINARY)
annotation class DelicateCardDetailsApi
