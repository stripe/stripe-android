package com.stripe.android.checkout

import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.checkout.injection.MerchantDisplayName
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutCommonConfigurationFactory @Inject constructor(
    @MerchantDisplayName private val merchantDisplayName: String,
) {
    fun create(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): CommonConfiguration {
        val billingDetailsCollectionConfiguration = configuration.paymentElementConfiguration
            .billingDetailsCollectionConfiguration
            .reconcile(checkoutSessionResponse.requiresBillingAddress)
            .asPaymentSheet()

        return CommonConfiguration(
            merchantDisplayName = merchantDisplayName,
            // The customer is provided by the checkout session, not client-side configuration.
            customer = ConfigurationDefaults.customer,
            googlePay = checkoutSessionResponse.merchantCountry?.let { merchantCountry ->
                configuration.googlePayConfiguration?.asPaymentSheet(merchantCountry)
            },
            link = ConfigurationDefaults.link,
            defaultBillingDetails = PaymentSheet.BillingDetails(
                address = collectedDetails.billingAddress?.asPaymentSheetAddress(),
                email = checkoutSessionResponse.customerEmail,
                name = collectedDetails.billingName,
                phone = collectedDetails.billingPhoneNumber,
            ),
            shippingDetails = AddressDetails(
                name = collectedDetails.shippingName,
                address = collectedDetails.shippingAddress?.asPaymentSheetAddress(),
                phoneNumber = collectedDetails.shippingPhoneNumber,
            ),
            allowsDelayedPaymentMethods = ConfigurationDefaults.allowsDelayedPaymentMethods,
            allowsPaymentMethodsRequiringShippingAddress =
                ConfigurationDefaults.allowsPaymentMethodsRequiringShippingAddress,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            preferredNetworks = ConfigurationDefaults.preferredNetworks,
            allowsRemovalOfLastSavedPaymentMethod = ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod,
            paymentMethodOrder = ConfigurationDefaults.paymentMethodOrder,
            externalPaymentMethods = ConfigurationDefaults.externalPaymentMethods,
            cardBrandAcceptance = ConfigurationDefaults.cardBrandAcceptance,
            allowedCardFundingTypes = ConfigurationDefaults.allowedCardFundingTypes,
            customPaymentMethods = ConfigurationDefaults.customPaymentMethods,
            googlePlacesApiKey = ConfigurationDefaults.googlePlacesApiKey,
            termsDisplay = emptyMap(),
            walletButtons = null,
            opensCardScannerAutomatically = ConfigurationDefaults.opensCardScannerAutomatically,
            userOverrideCountry = ConfigurationDefaults.userOverrideCountry,
            appearance = ConfigurationDefaults.appearance,
        )
    }
}

@OptIn(CheckoutSessionPreview::class)
internal fun Address.State.asPaymentSheetAddress(): PaymentSheet.Address {
    return PaymentSheet.Address(
        city = city,
        country = country,
        line1 = line1,
        line2 = line2,
        postalCode = postalCode,
        state = state,
    )
}

/**
 * Builds the [EmbeddedPaymentElement.Configuration] the embedded content UI renders from, directly out
 * of the checkout-owned [CheckoutController.Configuration.State] (plus the loaded
 * [checkoutSessionResponse] and any locally [collected details][collectedDetails]) — the same source
 * [CheckoutCommonConfigurationFactory] maps to the loader's [CommonConfiguration]. Derived on demand so
 * the state doesn't have to hold a second configuration object.
 */
@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.asEmbeddedPaymentElementConfiguration(
    merchantDisplayName: String,
    checkoutSessionResponse: CheckoutSessionResponse,
    collectedDetails: CheckoutCollectedDetails,
): EmbeddedPaymentElement.Configuration {
    val billingDetailsCollectionConfiguration = paymentElementConfiguration
        .billingDetailsCollectionConfiguration
        .reconcile(checkoutSessionResponse.requiresBillingAddress)
        .asPaymentSheet()

    return EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
        .embeddedViewDisplaysMandateText(paymentElementConfiguration.embeddedViewDisplaysMandateText)
        .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
        .googlePay(
            googlePay = checkoutSessionResponse.merchantCountry?.let { merchantCountry ->
                googlePayConfiguration?.asPaymentSheet(merchantCountry)
            }
        )
        .defaultBillingDetails(
            PaymentSheet.BillingDetails(
                address = collectedDetails.billingAddress?.asPaymentSheetAddress(),
                email = checkoutSessionResponse.customerEmail,
                name = collectedDetails.billingName,
                phone = collectedDetails.billingPhoneNumber,
            )
        )
        .shippingDetails(
            AddressDetails(
                name = collectedDetails.shippingName,
                address = collectedDetails.shippingAddress?.asPaymentSheetAddress(),
                phoneNumber = collectedDetails.shippingPhoneNumber,
            )
        )
        .build()
}
