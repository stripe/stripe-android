package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.paymentsheet.PaymentSheet

/**
 * Billing-address shape selected before form elements are created.
 */
internal sealed interface ResolvedBillingAddress {
    /** Renders no address fields and remains eligible for promotion by a later resolution stage. */
    data object Absent : ResolvedBillingAddress

    /**
     * Renders a country selector restricted to [allowedCountryCodes] and remains eligible for later promotion.
     */
    data class CountryOnly(
        val allowedCountryCodes: Set<String>,
    ) : ResolvedBillingAddress

    /** Renders a full address restricted to [allowedCountryCodes] and is preserved by later resolution stages. */
    data class Full(
        val allowedCountryCodes: Set<String>,
    ) : ResolvedBillingAddress

    /** Renders no address fields and prevents every later resolution stage from promoting address collection. */
    data object Suppressed : ResolvedBillingAddress
}

/**
 * Resolves billing-address policy in precedence order.
 *
 * First, the payment-method policy is combined with merchant collection settings. A later automatic-tax stage
 * may then promote only [ResolvedBillingAddress.Absent] and [ResolvedBillingAddress.CountryOnly].
 */
internal class BillingAddressPolicyResolver(
    private val policy: PaymentMethodBillingAddressPolicy?,
    private val addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
    private val merchantAllowedCountryCodes: Set<String>,
) {
    fun resolve(): ResolvedBillingAddress = resolvePaymentMethodPolicy()

    private fun resolvePaymentMethodPolicy(): ResolvedBillingAddress {
        return when (policy) {
            null -> when (addressCollectionMode) {
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                    ResolvedBillingAddress.Full(merchantAllowedCountryCodes)
                }
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                    ResolvedBillingAddress.Absent
                }
            }
            is PaymentMethodBillingAddressPolicy.CountryOnly -> when (addressCollectionMode) {
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                    ResolvedBillingAddress.Full(policy.allowedCountryCodes)
                }
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                    ResolvedBillingAddress.CountryOnly(policy.allowedCountryCodes)
                }
            }
            is PaymentMethodBillingAddressPolicy.FullAddressIfAllowed -> when (addressCollectionMode) {
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                    ResolvedBillingAddress.Full(policy.allowedCountryCodes)
                }
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                    ResolvedBillingAddress.Absent
                }
            }
            PaymentMethodBillingAddressPolicy.Suppressed -> ResolvedBillingAddress.Suppressed
        }
    }
}
