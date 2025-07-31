package com.stripe.android.elements.payment

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Address autocomplete support for MPE is beta. It may be changed in the future without notice."
)
@Retention(AnnotationRetention.BINARY)
annotation class AddressAutocompletePreview
