package com.stripe.android.checkout

import com.stripe.android.checkout.CheckoutController.Configuration
import com.stripe.android.checkout.injection.AppName
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration as PaymentSheetBillingDetails

@OptIn(CheckoutSessionPreview::class, CardFundingFilteringPrivatePreview::class)
internal class CheckoutCommonConfigurationFactory @Inject constructor(
    @AppName private val appName: String,
) {
    fun create(
        configuration: CheckoutController.Configuration.State,
        paymentElementConfiguration: PaymentElement.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): CommonConfiguration {
        return createCommonConfiguration(
            configuration = configuration,
            paymentElementConfiguration = paymentElementConfiguration,
            checkoutSessionResponse = checkoutSessionResponse,
            collectedDetails = collectedDetails,
            googlePayConfiguration = paymentElementConfiguration.toGooglePayConfiguration(checkoutSessionResponse),
            linkConfiguration = paymentElementConfiguration.linkConfiguration.asPaymentSheet(),
            billingDetailsCollectionConfiguration =
                checkoutSessionResponse.toBillingDetailsCollectionConfiguration(),
        )
    }

    fun createForExpressCheckoutElement(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): CommonConfiguration? {
        val expressCheckoutElementConfiguration = configuration.expressCheckoutElementConfiguration ?: return null
        return createCommonConfiguration(
            configuration = configuration,
            paymentElementConfiguration = configuration.paymentElementConfiguration,
            checkoutSessionResponse = checkoutSessionResponse,
            collectedDetails = collectedDetails,
            googlePayConfiguration =
                configuration.toExpressCheckoutElementGooglePayConfiguration(checkoutSessionResponse),
            linkConfiguration = expressCheckoutElementConfiguration.linkConfiguration.asPaymentSheet(),
            billingDetailsCollectionConfiguration = PaymentSheetBillingDetails(
                email = if (
                    checkoutSessionResponse.customerEmail == null && configuration.defaults.email == null
                ) {
                    PaymentSheetBillingDetails.CollectionMode.Always
                } else {
                    PaymentSheetBillingDetails.CollectionMode.Automatic
                },
                address = if (checkoutSessionResponse.requiresBillingAddress) {
                    PaymentSheetBillingDetails.AddressCollectionMode.Full
                } else {
                    PaymentSheetBillingDetails.AddressCollectionMode.Automatic
                },
                attachDefaultsToPaymentMethod = true,
            ),
        )
    }

    fun createForPaymentElement(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): CommonConfiguration? {
        val paymentElementConfiguration = configuration.paymentElementConfiguration ?: return null
        return create(
            configuration = configuration,
            paymentElementConfiguration = paymentElementConfiguration,
            checkoutSessionResponse = checkoutSessionResponse,
            collectedDetails = collectedDetails,
        )
    }

    private fun createCommonConfiguration(
        configuration: CheckoutController.Configuration.State,
        paymentElementConfiguration: PaymentElement.Configuration.State?,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
        googlePayConfiguration: PaymentSheet.GooglePayConfiguration?,
        linkConfiguration: PaymentSheet.LinkConfiguration,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    ): CommonConfiguration = CommonConfiguration(
        merchantDisplayName = configuration.resolveMerchantDisplayName(checkoutSessionResponse, appName),
        customer = ConfigurationDefaults.customer,
        googlePay = googlePayConfiguration,
        link = linkConfiguration,
        defaultBillingDetails = configuration.toBillingDetails(
            checkoutSessionResponse = checkoutSessionResponse,
            collectedEmail = collectedDetails.email,
        ),
        shippingDetails = collectedDetails.toShippingDetails(),
        allowsDelayedPaymentMethods = true,
        allowsPaymentMethodsRequiringShippingAddress = true,
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        preferredNetworks = paymentElementConfiguration?.preferredNetworks
            ?: ConfigurationDefaults.preferredNetworks,
        allowsRemovalOfLastSavedPaymentMethod = ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod,
        paymentMethodOrder = paymentElementConfiguration?.paymentMethodOrder
            ?: ConfigurationDefaults.paymentMethodOrder,
        externalPaymentMethods = ConfigurationDefaults.externalPaymentMethods,
        cardBrandAcceptance = paymentElementConfiguration?.cardBrandAcceptance?.asPaymentSheet()
            ?: ConfigurationDefaults.cardBrandAcceptance,
        allowedCardFundingTypes = ConfigurationDefaults.allowedCardFundingTypes,
        customPaymentMethods = ConfigurationDefaults.customPaymentMethods,
        googlePlacesApiKey = null,
        termsDisplay = paymentElementConfiguration?.termsDisplay?.asPaymentSheet() ?: emptyMap(),
        walletButtons = null,
        opensCardScannerAutomatically = paymentElementConfiguration?.opensCardScannerAutomatically
            ?: ConfigurationDefaults.opensCardScannerAutomatically,
        userOverrideCountry = ConfigurationDefaults.userOverrideCountry,
        appearance = paymentElementConfiguration?.appearance?.asPaymentSheet()
            ?: ConfigurationDefaults.appearance,
    )
}
