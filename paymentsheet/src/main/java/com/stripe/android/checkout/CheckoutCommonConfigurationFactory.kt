package com.stripe.android.checkout

import com.stripe.android.checkout.injection.AppName
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.paymentelement.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentelement.CheckoutSessionPreview
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
    ): CommonConfiguration = CommonConfiguration(
        merchantDisplayName = configuration.resolveMerchantDisplayName(checkoutSessionResponse, appName),
        customer = ConfigurationDefaults.customer,
        googlePay = configuration.toGooglePayConfiguration(checkoutSessionResponse),
        link = configuration.paymentElementConfiguration.linkConfiguration.asPaymentSheet(),
        defaultBillingDetails = collectedDetails.toBillingDetails(checkoutSessionResponse),
        shippingDetails = collectedDetails.toShippingDetails(),
        allowsDelayedPaymentMethods = ConfigurationDefaults.allowsDelayedPaymentMethods,
        allowsPaymentMethodsRequiringShippingAddress =
            ConfigurationDefaults.allowsPaymentMethodsRequiringShippingAddress,
        billingDetailsCollectionConfiguration =
            configuration.toBillingDetailsCollectionConfiguration(checkoutSessionResponse),
        preferredNetworks = configuration.paymentElementConfiguration.preferredNetworks,
        allowsRemovalOfLastSavedPaymentMethod = ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod,
        paymentMethodOrder = configuration.paymentElementConfiguration.paymentMethodOrder,
        externalPaymentMethods = ConfigurationDefaults.externalPaymentMethods,
        cardBrandAcceptance = configuration.paymentElementConfiguration.cardBrandAcceptance.asPaymentSheet(),
        allowedCardFundingTypes = configuration.paymentElementConfiguration.allowedCardFundingTypes.asPaymentSheet(),
        customPaymentMethods = ConfigurationDefaults.customPaymentMethods,
        googlePlacesApiKey = null,
        termsDisplay = configuration.paymentElementConfiguration.termsDisplay.asPaymentSheet(),
        walletButtons = null,
        opensCardScannerAutomatically = configuration.paymentElementConfiguration.opensCardScannerAutomatically,
        userOverrideCountry = ConfigurationDefaults.userOverrideCountry,
        appearance = configuration.paymentElementConfiguration.appearance.asPaymentSheet(),
    )
}
