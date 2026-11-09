package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.LinkDisabledState
import com.stripe.android.paymentsheet.state.LinkState

internal fun logUnavailableLink(
    metadata: PaymentMethodMetadata,
    logger: UserFacingLogger,
    isExpressCheckoutElement: Boolean,
) {
    val linkState = metadata.linkStateResult ?: return
    val reasons = buildList {
        when (linkState) {
            is LinkDisabledState -> addAll(linkState.linkDisabledReasons.map { it.errorMessage })
            is LinkState -> if (!metadata.shouldShowLinkButton) {
                when (metadata.linkConfiguration.display) {
                    PaymentSheet.LinkConfiguration.Display.Automatic -> Unit
                    PaymentSheet.LinkConfiguration.Display.Never -> add(
                        LinkDisabledReason.LinkConfiguration.errorMessage
                    )
                    PaymentSheet.LinkConfiguration.Display.WalletButtonHidden -> add(
                        "LinkConfiguration.display is set to WalletButtonHidden and no existing Link user was found."
                    )
                }
            }
        }
        val checkoutSession = metadata.integrationMetadata as? IntegrationMetadata.CheckoutSession
        if (isExpressCheckoutElement && checkoutSession?.checkoutSessionResponse?.requiresShippingAddress == true) {
            add("The Link button in Express Checkout Element does not support collecting a shipping address.")
        }
    }
    if (reasons.isNotEmpty()) {
        val unavailableMessage = when (linkState) {
            is LinkDisabledState -> "Payment Element could not offer Link:"
            is LinkState -> "Payment Element could not show the Link button:"
        }
        logger.logWarningWithoutPii("$unavailableMessage\n" + reasons.joinToString("\n") { "  * $it" })
    }
}

private val LinkDisabledReason.errorMessage: String
    get() = when (this) {
        LinkDisabledReason.NotSupportedInElementsSession -> "Link is not supported by this Elements Session."
        LinkDisabledReason.LinkConfiguration -> "LinkConfiguration.display is set to Never."
        LinkDisabledReason.CardBrandFiltering ->
            "Card brand filtering requires native Link; the web Link flow does not support it."
        LinkDisabledReason.BillingDetailsCollection ->
            "Collecting additional billing details requires native Link; the web Link flow does not support it."
        LinkDisabledReason.AutomaticTaxBillingAddress ->
            "Checkout Sessions with automatic tax using the billing address do not support Link."
        LinkDisabledReason.CheckoutSessionsRequiresEmail ->
            "Web Link in passthrough mode requires a customer email for Checkout Sessions. " +
                "Provide a customer email on the Checkout Session or in defaultBillingDetails."
    }
