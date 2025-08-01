package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.common.model.containsVolatileDifferences
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.allowedWalletTypes
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState
import javax.inject.Inject

internal fun interface PaymentSelectionUpdater {
    operator fun invoke(
        existingSelection: PaymentSelection?,
        previousConfig: PaymentSheet.Configuration?,
        newState: PaymentSheetState.Full,
        newConfig: PaymentSheet.Configuration,
        walletButtonsAlreadyShown: Boolean,
    ): PaymentSelection?
}

internal class DefaultPaymentSelectionUpdater @Inject constructor() : PaymentSelectionUpdater {

    override operator fun invoke(
        existingSelection: PaymentSelection?,
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
        val candidateSelection = existingSelection ?: newState.paymentSelection

        return candidateSelection?.takeIf { selectionToValidate ->
            canUseSelection(selectionToValidate, newState, newConfig, walletButtonsAlreadyShown, existingSelection) &&
                previousConfig?.let { previousConfig ->
                    !previousConfig.asCommonConfiguration()
                        .containsVolatileDifferences(newConfig.asCommonConfiguration())
                } != false
        }
    }

    private fun canUseSelection(
        selectionToValidate: PaymentSelection,
        state: PaymentSheetState.Full,
        configuration: PaymentSheet.Configuration,
        walletButtonsAlreadyShown: Boolean,
        existingSelection: PaymentSelection?,
    ): Boolean {
        // The types that are allowed for this intent, as returned by the backend
        val allowedTypes = state.paymentMethodMetadata.supportedPaymentMethodTypes()

        return when (selectionToValidate) {
            is PaymentSelection.New -> {
                val requiresMandate = shouldAskForMandate(
                    currentSelection = selectionToValidate,
                    metadata = state.paymentMethodMetadata,
                )
                val code = selectionToValidate.paymentMethodCreateParams.typeCode
                code in allowedTypes && !requiresMandate
            }
            is PaymentSelection.Saved -> {
                val paymentMethod = selectionToValidate.paymentMethod
                val code = paymentMethod.type?.code
                code in allowedTypes && paymentMethod in (state.customer?.paymentMethods ?: emptyList())
            }
            is PaymentSelection.GooglePay -> {
                state.paymentMethodMetadata.isGooglePayReady &&
                    walletCanBeUsed(selectionToValidate, configuration, walletButtonsAlreadyShown, existingSelection)
            }
            is PaymentSelection.Link -> {
                state.paymentMethodMetadata.linkState != null &&
                    walletCanBeUsed(selectionToValidate, configuration, walletButtonsAlreadyShown, existingSelection)
            }
            is PaymentSelection.ExternalPaymentMethod -> {
                state.paymentMethodMetadata.isExternalPaymentMethod(selectionToValidate.type)
            }
            is PaymentSelection.CustomPaymentMethod -> {
                state.paymentMethodMetadata.isCustomPaymentMethod(selectionToValidate.id)
            }
            is PaymentSelection.ShopPay -> {
                false
            }
        }
    }

    private fun walletCanBeUsed(
        selectionToValidate: PaymentSelection,
        configuration: PaymentSheet.Configuration,
        walletButtonsAlreadyShown: Boolean,
        existingSelection: PaymentSelection?
    ): Boolean {
        if (!configuration.walletButtons.willDisplayExternally && !walletButtonsAlreadyShown) {
            return true
        }

        val walletTypesDisplayedExternally = configuration.walletButtons.allowedWalletTypes

        val walletType = when (selectionToValidate) {
            is PaymentSelection.GooglePay -> WalletType.GooglePay
            is PaymentSelection.Link -> WalletType.Link
            else -> null
        }

        // If this selection is the existing selection (likely selected externally)
        // AND specific wallets are configured externally, preserve it
        if (selectionToValidate == existingSelection && walletType in walletTypesDisplayedExternally) {
            return true
        }

        // Allow wallets that are NOT in the external list
        // (because external wallets are handled separately to avoid duplication)
        return walletType != null && !walletTypesDisplayedExternally.contains(walletType)
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
            false
        }
    }
}

private val PaymentSelection.New.customerAcknowledgedMandate: Boolean
    get() = paymentMethodCreateParams.requiresMandate()
