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
        currentSelection: PaymentSelection?,
        previousConfig: PaymentSheet.Configuration?,
        newState: PaymentSheetState.Full,
        newConfig: PaymentSheet.Configuration,
        walletButtonsAlreadyShown: Boolean,
    ): PaymentSelection?
}

internal class DefaultPaymentSelectionUpdater @Inject constructor() : PaymentSelectionUpdater {

    override operator fun invoke(
        currentSelection: PaymentSelection?,
        previousConfig: PaymentSheet.Configuration?,
        newState: PaymentSheetState.Full,
        newConfig: PaymentSheet.Configuration,
        walletButtonsAlreadyShown: Boolean,
    ): PaymentSelection? {
        val availableSelection = currentSelection ?: newState.paymentSelection

        return availableSelection?.takeIf { selection ->
            canUseSelection(selection, newState, newConfig, walletButtonsAlreadyShown) &&
                previousConfig?.let { previousConfig ->
                    !previousConfig.asCommonConfiguration()
                        .containsVolatileDifferences(newConfig.asCommonConfiguration())
                } != false
        }
    }

    private fun canUseSelection(
        selection: PaymentSelection,
        state: PaymentSheetState.Full,
        configuration: PaymentSheet.Configuration,
        walletButtonsAlreadyShown: Boolean,
    ): Boolean {
        // The types that are allowed for this intent, as returned by the backend
        val allowedTypes = state.paymentMethodMetadata.supportedPaymentMethodTypes()

        return when (selection) {
            is PaymentSelection.New -> {
                val requiresMandate = shouldAskForMandate(
                    currentSelection = selection,
                    metadata = state.paymentMethodMetadata,
                )
                val code = selection.paymentMethodCreateParams.typeCode
                code in allowedTypes && !requiresMandate
            }
            is PaymentSelection.Saved -> {
                val paymentMethod = selection.paymentMethod
                val code = paymentMethod.type?.code
                code in allowedTypes && paymentMethod in (state.customer?.paymentMethods ?: emptyList())
            }
            is PaymentSelection.GooglePay -> {
                state.paymentMethodMetadata.isGooglePayReady &&
                    walletCanBeUsed(selection, configuration, walletButtonsAlreadyShown)
            }
            is PaymentSelection.Link -> {
                state.paymentMethodMetadata.linkState != null &&
                    walletCanBeUsed(selection, configuration, walletButtonsAlreadyShown)
            }
            is PaymentSelection.ExternalPaymentMethod -> {
                state.paymentMethodMetadata.isExternalPaymentMethod(selection.type)
            }
            is PaymentSelection.CustomPaymentMethod -> {
                state.paymentMethodMetadata.isCustomPaymentMethod(selection.id)
            }
            is PaymentSelection.ShopPay -> {
                false
            }
        }
    }

    private fun walletCanBeUsed(
        selection: PaymentSelection,
        configuration: PaymentSheet.Configuration,
        walletButtonsAlreadyShown: Boolean
    ): Boolean {
        if (!configuration.walletButtons.willDisplayExternally && !walletButtonsAlreadyShown) {
            return true
        }

        val walletTypesDisplayedExternally = configuration.walletButtons.allowedWalletTypes

        val walletType = when (selection) {
            is PaymentSelection.GooglePay -> WalletType.GooglePay
            is PaymentSelection.Link -> WalletType.Link
            else -> null
        }

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
