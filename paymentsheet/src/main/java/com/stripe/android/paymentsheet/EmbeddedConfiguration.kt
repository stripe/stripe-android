package com.stripe.android.paymentsheet

import android.os.Bundle
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.paymentelement.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode

@OptIn(
    ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class,
    CardFundingFilteringPrivatePreview::class,
    com.stripe.android.paymentsheet.CardFundingFilteringPrivatePreview::class,
)
internal fun PaymentSheet.Configuration.asEmbeddedConfiguration(
    formSheetAction: EmbeddedPaymentElement.FormSheetAction,
): EmbeddedPaymentElement.Configuration {
    return EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
        .customer(customer)
        .googlePay(googlePay)
        .defaultBillingDetails(defaultBillingDetails)
        .shippingDetails(shippingDetails)
        .allowsDelayedPaymentMethods(allowsDelayedPaymentMethods)
        .allowsPaymentMethodsRequiringShippingAddress(allowsPaymentMethodsRequiringShippingAddress)
        .appearance(appearance)
        .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
        .preferredNetworks(preferredNetworks)
        .allowsRemovalOfLastSavedPaymentMethod(allowsRemovalOfLastSavedPaymentMethod)
        .paymentMethodOrder(paymentMethodOrder)
        .externalPaymentMethods(externalPaymentMethods)
        .cardBrandAcceptance(cardBrandAcceptance)
        .allowedCardFundingTypes(allowedCardFundingTypes)
        .customPaymentMethods(customPaymentMethods)
        .link(link)
        .formSheetAction(formSheetAction)
        .termsDisplay(termsDisplay)
        .opensCardScannerAutomatically(opensCardScannerAutomatically)
        .userOverrideCountry(userOverrideCountry)
        .googlePlacesApiKey(googlePlacesApiKey)
        .apply {
            primaryButtonLabel?.let(::primaryButtonLabel)
        }
        .build()
}

internal fun PaymentOptionContract.Args.asEmbeddedActivityArgs(): EmbeddedActivityArgs {
    return EmbeddedActivityArgs(
        paymentMethodMetadata = state.paymentMethodMetadata,
        configuration = configuration.asEmbeddedConfiguration(
            EmbeddedPaymentElement.FormSheetAction.Continue
        ),
        paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
        statusBarColor = null,
        selection = state.paymentSelection,
        previousNewSelections = Bundle(),
        customerState = state.customer,
        promotions = promotions,
        launchMode = EmbeddedLaunchMode.PaymentOptions,
    )
}
