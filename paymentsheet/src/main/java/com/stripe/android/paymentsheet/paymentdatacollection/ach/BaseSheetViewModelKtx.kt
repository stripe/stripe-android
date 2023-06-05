package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.content.Context
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState.BillingDetailsCollection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal fun BaseSheetViewModel.handleScreenStateChanged(
    context: Context,
    screenState: USBankAccountFormScreenState,
    enabled: Boolean,
    merchantName: String,
    onPrimaryButtonClick: (USBankAccountFormScreenState) -> Unit,
) {
    onError(screenState.error)

    val completePayment = this is PaymentSheetViewModel
    val showProcessingWhenClicked = screenState is BillingDetailsCollection || completePayment

    updatePrimaryButton(
        text = screenState.primaryButtonText,
        onClick = { onPrimaryButtonClick(screenState) },
        enabled = enabled,
        shouldShowProcessingWhenClicked = showProcessingWhenClicked
    )

    updateMandateText(
        context = context,
        screenState = screenState,
        mandateText = screenState.mandateText,
        merchantName = merchantName,
    )
}

private fun BaseSheetViewModel.updatePrimaryButton(
    text: String,
    onClick: () -> Unit,
    shouldShowProcessingWhenClicked: Boolean,
    enabled: Boolean,
) {
    updatePrimaryButtonState(PrimaryButton.State.Ready)
    updateCustomPrimaryButtonUiState {
        PrimaryButton.UIState(
            label = text,
            onClick = {
                if (shouldShowProcessingWhenClicked) {
                    updatePrimaryButtonState(PrimaryButton.State.StartProcessing)
                }
                onClick()
                updateCustomPrimaryButtonUiState { button ->
                    button?.copy(enabled = false)
                }
            },
            enabled = enabled,
            lockVisible = this is PaymentSheetViewModel,
        )
    }
}

internal fun BaseSheetViewModel.updateMandateText(
    context: Context,
    screenState: USBankAccountFormScreenState,
    mandateText: String?,
    merchantName: String,
) {
    val microdepositsText =
        if (screenState is USBankAccountFormScreenState.VerifyWithMicrodeposits) {
            context.getString(R.string.stripe_paymentsheet_microdeposit, merchantName)
        } else {
            ""
        }

    val updatedText = mandateText?.let {
        """
            $microdepositsText
                
            $mandateText
        """.trimIndent()
    }

    updateBelowButtonText(updatedText)
}
