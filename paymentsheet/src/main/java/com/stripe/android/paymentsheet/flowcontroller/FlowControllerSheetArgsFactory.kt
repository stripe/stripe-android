package com.stripe.android.paymentsheet.flowcontroller

import android.os.Bundle
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.configType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState

internal object FlowControllerSheetArgsFactory {
    @Suppress("LongParameterList")
    fun create(
        paymentSheetState: PaymentSheetState.Full,
        configuration: PaymentSheet.Configuration,
        paymentSelection: PaymentSelection?,
        linkAccountInfo: LinkAccountUpdate.Value,
        walletButtonsRendered: Boolean,
        productUsage: Set<String>,
        paymentElementCallbackIdentifier: String,
        promotions: List<PaymentMethodMessagePromotion>?,
    ): EmbeddedActivityArgs {
        return EmbeddedActivityArgs(
            paymentMethodMetadata = paymentSheetState.paymentMethodMetadata,
            configuration = configuration.asEmbeddedSheetConfiguration(),
            productUsage = productUsage,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            // FlowController confirms with its own status bar color; the sheet only selects.
            statusBarColor = null,
            selection = paymentSelection,
            previousNewSelections = Bundle(),
            customerState = paymentSheetState.customer,
            linkAccountInfo = linkAccountInfo,
            promotions = promotions.orEmpty(),
            launchMode = EmbeddedLaunchMode.PaymentOptions(
                flowController = EmbeddedLaunchMode.FlowControllerOptions(
                    visibleWallets = configuration.walletButtons.walletsVisibleInPaymentOptions(
                        walletButtonsRendered = walletButtonsRendered,
                    ),
                    googlePlacesApiKey = configuration.googlePlacesApiKey,
                ),
            ),
            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
        )
    }
}

/**
 * Wallets shown in the payment options sheet. Wallets with automatic visibility are only shown when the wallet buttons
 * aren't rendered, or when the wallet buttons never show them.
 */
internal fun PaymentSheet.WalletButtonsConfiguration.walletsVisibleInPaymentOptions(
    walletButtonsRendered: Boolean,
): List<WalletType> {
    return WalletType.entries.filter { walletType ->
        when (visibility.paymentElement[walletType.configType]) {
            null,
            PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility.Automatic -> {
                !walletButtonsRendered ||
                    visibility.walletButtonsView[walletType.configType] ==
                    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Never
            }
            PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility.Always -> true
            PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility.Never -> false
        }
    }
}

/**
 * The sheet only selects a payment option for FlowController, which confirms later, so the form sheet action is always
 * [EmbeddedPaymentElement.FormSheetAction.Continue].
 */
internal fun PaymentSheet.Configuration.asEmbeddedSheetConfiguration(): EmbeddedPaymentElement.Configuration {
    return EmbeddedPaymentElement.Configuration(
        merchantDisplayName = merchantDisplayName,
        customer = customer,
        googlePay = googlePay,
        defaultBillingDetails = defaultBillingDetails,
        shippingDetails = shippingDetails,
        allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
        allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
        appearance = appearance,
        primaryButtonLabel = primaryButtonLabel,
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        preferredNetworks = preferredNetworks,
        allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
        paymentMethodOrder = paymentMethodOrder,
        externalPaymentMethods = externalPaymentMethods,
        cardBrandAcceptance = cardBrandAcceptance,
        allowedCardFundingTypes = allowedCardFundingTypes,
        customPaymentMethods = customPaymentMethods,
        embeddedViewDisplaysMandateText = true,
        link = link,
        formSheetAction = EmbeddedPaymentElement.FormSheetAction.Continue,
        termsDisplay = termsDisplay,
        opensCardScannerAutomatically = opensCardScannerAutomatically,
        userOverrideCountry = userOverrideCountry,
        apiConfiguration = apiConfiguration,
    )
}
