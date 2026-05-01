package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.common.model.containsVolatileDifferences
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState
import javax.inject.Inject

internal fun interface PaymentSelectionUpdater {
    operator fun invoke(
        selection: PaymentSelection?,
        previousConfig: PaymentSheet.Configuration?,
        newState: PaymentSheetState.Full,
        newConfig: PaymentSheet.Configuration,
        walletButtonsAlreadyShown: Boolean,
    ): PaymentSelection?
}

internal class DefaultPaymentSelectionUpdater @Inject constructor() : PaymentSelectionUpdater {

    override operator fun invoke(
        selection: PaymentSelection?,
        previousConfig: PaymentSheet.Configuration?,
        newState: PaymentSheetState.Full,
        newConfig: PaymentSheet.Configuration,
        walletButtonsAlreadyShown: Boolean,
    ): PaymentSelection? {
        // Use existing selection if available, otherwise fall back to the state's initial selection:
        // 1. Customer's default payment method (if feature enabled)
        // 2. Previously saved selection from local storage/preferences (Google Pay, Link, or saved PM)
        // 3. First available customer payment method
        // 4. Google Pay (if available as fallback)
        val potentialSelection = selection ?: newState.paymentSelection

        return potentialSelection?.takeIf {
            canUseSelection(it, newState) && previousConfig?.let { previousConfig ->
                !previousConfig.asCommonConfiguration()
                    .containsVolatileDifferences(newConfig.asCommonConfiguration())
            } != false
        }
    }

    private fun canUseSelection(
        potentialSelection: PaymentSelection,
        state: PaymentSheetState.Full,
    ): Boolean {
        // The types that are allowed for this intent, as returned by the backend
        val allowedTypes = state.paymentMethodMetadata.supportedPaymentMethodTypes

        return when (potentialSelection) {
            is PaymentSelection.New -> {
                val requiresMandate = shouldAskForMandate(
                    currentSelection = potentialSelection,
                    metadata = state.paymentMethodMetadata,
                )
                val code = potentialSelection.paymentMethodCreateParams.typeCode
                code in allowedTypes && !requiresMandate
            }
            is PaymentSelection.Saved -> {
                val paymentMethod = potentialSelection.paymentMethod
                val code = paymentMethod.type?.code
                code in allowedTypes && paymentMethod in (state.customer?.paymentMethods ?: emptyList())
            }
            is PaymentSelection.GooglePay -> {
                state.paymentMethodMetadata.isGooglePayReady
            }
            is PaymentSelection.Link -> {
                state.paymentMethodMetadata.linkState != null
            }
            is PaymentSelection.ExternalPaymentMethod -> {
                state.paymentMethodMetadata.isExternalPaymentMethod(potentialSelection.type)
            }
            is PaymentSelection.CustomPaymentMethod -> {
                state.paymentMethodMetadata.isCustomPaymentMethod(potentialSelection.id)
            }
            is PaymentSelection.ShopPay -> {
                false
            }
        }
    }

    private fun shouldAskForMandate(
        currentSelection: PaymentSelection.New,
        metadata: PaymentMethodMetadata,
    ): Boolean {
        val code = currentSelection.paymentMethodCreateParams.typeCode

        val paymentMethodRequiresMandate = metadata.requiresMandate(code)

        return if (paymentMethodRequiresMandate) {
            !currentSelection.customerAcknowledgedMandate
        } else {
            needsMandateDisplayForCard(currentSelection, metadata)
        }
    }

    /**
     * Cards don't require an API-level mandate, but the form shows mandate text when
     * setupFutureUsage is set. If SFU was dynamically added after the user entered their card
     * details (via configureWithIntentConfiguration update), we need to invalidate the selection
     * so the user is forced to re-open the payment sheet and see the mandate text.
     */
    private fun needsMandateDisplayForCard(
        selection: PaymentSelection.New,
        metadata: PaymentMethodMetadata,
    ): Boolean {
        if (selection !is PaymentSelection.New.Card) return false

        val cardCode = PaymentMethod.Type.Card.code
        val wouldShowMandate = metadata.forceSetupFutureUseBehaviorAndNewMandate ||
            (metadata.hasIntentToSetup(cardCode) && metadata.mandateAllowed(PaymentMethod.Type.Card))

        if (!wouldShowMandate) return false

        val cardOptions = selection.paymentMethodOptionsParams as? PaymentMethodOptionsParams.Card
        return cardOptions?.setupFutureUsage == null
    }
}

private val PaymentSelection.New.customerAcknowledgedMandate: Boolean
    get() = paymentMethodCreateParams.requiresMandate()
