package com.stripe.android.checkout

import com.stripe.android.checkout.injection.AppName
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutCommonConfigurationFactory @Inject constructor(
    @AppName private val appName: String,
) {
    fun create(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): CommonConfiguration = CommonConfiguration(
        merchantDisplayName = configuration.resolveMerchantDisplayName(checkoutSessionResponse, appName),
        customer = ConfigurationDefaults.customer,
        googlePay = configuration.toGooglePayConfiguration(checkoutSessionResponse),
        link = configuration.linkConfiguration.asPaymentSheet(),
        defaultBillingDetails = collectedDetails.toBillingDetails(checkoutSessionResponse),
        shippingDetails = collectedDetails.toShippingDetails(),
        allowsDelayedPaymentMethods = ConfigurationDefaults.allowsDelayedPaymentMethods,
        allowsPaymentMethodsRequiringShippingAddress =
            ConfigurationDefaults.allowsPaymentMethodsRequiringShippingAddress,
        billingDetailsCollectionConfiguration =
            configuration.toBillingDetailsCollectionConfiguration(checkoutSessionResponse),
        preferredNetworks = ConfigurationDefaults.preferredNetworks,
        allowsRemovalOfLastSavedPaymentMethod = ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod,
        paymentMethodOrder = ConfigurationDefaults.paymentMethodOrder,
        externalPaymentMethods = ConfigurationDefaults.externalPaymentMethods,
        cardBrandAcceptance = ConfigurationDefaults.cardBrandAcceptance,
        allowedCardFundingTypes = ConfigurationDefaults.allowedCardFundingTypes,
        customPaymentMethods = ConfigurationDefaults.customPaymentMethods,
        googlePlacesApiKey = null,
        termsDisplay = configuration.paymentElementConfiguration.termsDisplay.asPaymentSheet(),
        walletButtons = null,
        opensCardScannerAutomatically = ConfigurationDefaults.opensCardScannerAutomatically,
        userOverrideCountry = ConfigurationDefaults.userOverrideCountry,
        appearance = configuration.paymentElementConfiguration.appearance.asPaymentSheet(),
    )
}
