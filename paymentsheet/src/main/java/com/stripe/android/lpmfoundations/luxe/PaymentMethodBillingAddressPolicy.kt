package com.stripe.android.lpmfoundations.luxe

/**
 * Billing-address policy contributed by a payment-method definition.
 */
internal sealed interface PaymentMethodBillingAddressPolicy {
    /**
     * Requires a country value from [allowedCountryCodes], unless merchant settings promote collection to a
     * full address.
     */
    data class CountryOnly(
        val allowedCountryCodes: Set<String>,
    ) : PaymentMethodBillingAddressPolicy

    /**
     * Collects a full address from [allowedCountryCodes] when merchant settings allow address collection.
     */
    data class FullAddressIfAllowed(
        val allowedCountryCodes: Set<String>,
    ) : PaymentMethodBillingAddressPolicy

    /** Prevents this payment method from collecting a billing address in every resolution stage. */
    data object Suppressed : PaymentMethodBillingAddressPolicy
}
