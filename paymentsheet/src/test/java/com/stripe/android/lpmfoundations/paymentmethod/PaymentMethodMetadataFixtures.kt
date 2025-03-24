package com.stripe.android.lpmfoundations.paymentmethod

internal object PaymentMethodMetadataFixtures {

    internal val DEFAULT_CUSTOMER_METADATA_PERMISSIONS = CustomerMetadata.Permissions(
        canRemovePaymentMethods = true,
        canRemoveLastPaymentMethod = true,
        canRemoveDuplicates = true,
    )

    internal val DEFAULT_CUSTOMER_METADATA = CustomerMetadata(
        hasCustomerConfiguration = true,
        isPaymentMethodSetAsDefaultEnabled = false,
        permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS
    )
}
