package com.stripe.android.checkout

import com.stripe.android.checkout.injection.AppName
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.elements.ece.asPaymentSheet
import com.stripe.android.paymentelement.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class, CardFundingFilteringPrivatePreview::class)
internal class CheckoutCommonConfigurationFactory @Inject constructor(
    @AppName private val appName: String,
) {
    fun create(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): CommonConfiguration = createCommonConfiguration(
        configuration = configuration,
        checkoutSessionResponse = checkoutSessionResponse,
        collectedDetails = collectedDetails,
        googlePayConfiguration =
            configuration.toExpressCheckoutElementGooglePayConfiguration(checkoutSessionResponse),
        linkConfiguration = configuration.paymentElementConfiguration.linkConfiguration.asPaymentSheet(),
        billingDetailsCollectionConfiguration =
            configuration.toBillingDetailsCollectionConfiguration(checkoutSessionResponse),
    )

    fun createForExpressCheckoutElement(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): CommonConfiguration = createCommonConfiguration(
        configuration = configuration,
        checkoutSessionResponse = checkoutSessionResponse,
        collectedDetails = collectedDetails,
        googlePayConfiguration =
            configuration.toExpressCheckoutElementGooglePayConfiguration(checkoutSessionResponse),
        linkConfiguration = configuration.expressCheckoutElementConfiguration.linkConfiguration.asPaymentSheet(),
        billingDetailsCollectionConfiguration = configuration.expressCheckoutElementConfiguration
            .billingDetailsCollectionConfiguration
            .asPaymentSheet(requiresBillingAddress = checkoutSessionResponse.requiresBillingAddress),
    )

    fun createForPaymentElement(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): CommonConfiguration = createCommonConfiguration(
        configuration = configuration,
        checkoutSessionResponse = checkoutSessionResponse,
        collectedDetails = collectedDetails,
        googlePayConfiguration = configuration.toPaymentElementGooglePayConfiguration(checkoutSessionResponse),
        linkConfiguration = configuration.paymentElementConfiguration.linkConfiguration.asPaymentSheet(),
        billingDetailsCollectionConfiguration =
            configuration.toBillingDetailsCollectionConfiguration(checkoutSessionResponse),
    )

    private fun createCommonConfiguration(
        configuration: CheckoutController.Configuration.State,
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
        defaultBillingDetails = collectedDetails.toBillingDetails(checkoutSessionResponse),
        shippingDetails = collectedDetails.toShippingDetails(),
        allowsDelayedPaymentMethods = true,
        allowsPaymentMethodsRequiringShippingAddress = true,
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        preferredNetworks = configuration.paymentElementConfiguration.preferredNetworks,
        allowsRemovalOfLastSavedPaymentMethod = ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod,
        paymentMethodOrder = configuration.paymentElementConfiguration.paymentMethodOrder,
        externalPaymentMethods = ConfigurationDefaults.externalPaymentMethods,
        cardBrandAcceptance = configuration.paymentElementConfiguration.cardBrandAcceptance.asPaymentSheet(),
        allowedCardFundingTypes = ConfigurationDefaults.allowedCardFundingTypes,
        customPaymentMethods = ConfigurationDefaults.customPaymentMethods,
        googlePlacesApiKey = null,
        termsDisplay = configuration.paymentElementConfiguration.termsDisplay.asPaymentSheet(),
        walletButtons = null,
        opensCardScannerAutomatically = configuration.paymentElementConfiguration.opensCardScannerAutomatically,
        userOverrideCountry = ConfigurationDefaults.userOverrideCountry,
        appearance = configuration.paymentElementConfiguration.appearance.asPaymentSheet(),
    )
}
