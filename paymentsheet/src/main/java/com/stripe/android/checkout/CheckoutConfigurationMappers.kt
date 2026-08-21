package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressFormatParser
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.validateShippingCountry
import com.stripe.android.uicore.elements.IdentifierSpec

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.toBillingDetailsCollectionConfiguration(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.BillingDetailsCollectionConfiguration =
    paymentElementConfiguration.billingDetailsCollectionConfiguration
        .reconcile(checkoutSessionResponse.requiresBillingAddress)
        .asPaymentSheet()

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.resolveMerchantDisplayName(
    checkoutSessionResponse: CheckoutSessionResponse,
    appName: String,
): String = merchantDisplayName ?: checkoutSessionResponse.businessName ?: appName

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.toExpressCheckoutElementGooglePayConfiguration(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.GooglePayConfiguration? =
    checkoutSessionResponse.merchantCountry?.let { merchantCountry ->
        expressCheckoutElementConfiguration.googlePayConfiguration.asPaymentSheet(
            merchantCountry = merchantCountry,
            liveMode = checkoutSessionResponse.liveMode,
            isDebugBuild = BuildConfig.DEBUG,
        )
    }

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.toPaymentElementGooglePayConfiguration(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.GooglePayConfiguration? =
    checkoutSessionResponse.merchantCountry?.let { merchantCountry ->
        paymentElementConfiguration.googlePayConfiguration.asPaymentSheet(
            merchantCountry = merchantCountry,
            liveMode = checkoutSessionResponse.liveMode,
            isDebugBuild = BuildConfig.DEBUG,
        )
    }

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutCollectedDetails.toBillingDetails(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.BillingDetails = PaymentSheet.BillingDetails(
    address = billingAddress?.asPaymentSheet(),
    email = resolveEmail(checkoutSessionResponse),
    name = billingName,
)

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutCollectedDetails.resolveEmail(
    response: CheckoutSessionResponse,
): String? = email ?: response.customerEmail

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutCollectedDetails.toShippingDetails(): AddressDetails = AddressDetails(
    name = shippingName,
    address = shippingAddress?.asPaymentSheet(),
)

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.normalizeShippingDefaults(
    checkoutSessionResponse: CheckoutSessionResponse,
): CheckoutController.Configuration.State {
    val shippingDetails = defaults.shippingDetails ?: return this
    val address = shippingDetails.address ?: return this
    if (checkoutSessionResponse.validateShippingCountry(address.country).isFailure) {
        return copy(defaults = defaults.copy(shippingDetails = null))
    }

    val addressDetails = AddressDetails(
        name = shippingDetails.name,
        address = address.asPaymentSheet(),
    )
    val normalizedValues = AddressFormatParser(
        AddressLauncher.Configuration(
            allowedCountries = checkoutSessionResponse.allowedShippingCountries?.toSet()
                ?: emptySet(),
        )
    ).parse(addressDetails)
    val normalizedAddress = CheckoutController.Address()
        .city(normalizedValues[IdentifierSpec.City])
        .country(requireNotNull(normalizedValues[IdentifierSpec.Country]))
        .line1(normalizedValues[IdentifierSpec.Line1])
        .line2(normalizedValues[IdentifierSpec.Line2])
        .postalCode(normalizedValues[IdentifierSpec.PostalCode])
        .state(normalizedValues[IdentifierSpec.State])
        .build()
    val normalizedDetails = shippingDetails.copy(
        name = normalizedValues[IdentifierSpec.Name],
        address = normalizedAddress,
    )

    return copy(defaults = defaults.copy(shippingDetails = normalizedDetails))
}
