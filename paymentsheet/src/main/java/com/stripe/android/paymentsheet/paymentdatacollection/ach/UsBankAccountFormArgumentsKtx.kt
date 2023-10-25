package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.content.Context
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState.BillingDetailsCollection
import com.stripe.android.paymentsheet.ui.PrimaryButton

internal fun USBankAccountFormArguments.handleScreenStateChanged(
    context: Context,
    screenState: USBankAccountFormScreenState,
    enabled: Boolean,
    merchantName: String,
    onPrimaryButtonClick: (USBankAccountFormScreenState) -> Unit,
    onPrimaryButtonStateChanged: (PrimaryButton.State) -> Unit,
) {
    screenState.error?.let {
        onError(context.getString(it))
    }

    val showProcessingWhenClicked = screenState is BillingDetailsCollection || isCompleteFlow

    updatePrimaryButton(
        text = screenState.primaryButtonText,
        onClick = { onPrimaryButtonClick(screenState) },
        onStateChanged = { onPrimaryButtonStateChanged(it) },
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

private fun USBankAccountFormArguments.updatePrimaryButton(
    text: String,
    onClick: () -> Unit,
    onStateChanged: (PrimaryButton.State) -> Unit,
    shouldShowProcessingWhenClicked: Boolean,
    enabled: Boolean,
) {
    onUpdatePrimaryButtonUIState {
        PrimaryButton.UIState(
            label = text,
            onClick = {
                if (shouldShowProcessingWhenClicked) {
                    onStateChanged(PrimaryButton.State.StartProcessing)
                    onUpdatePrimaryButtonState(PrimaryButton.State.StartProcessing)
                }
                onClick()
                onUpdatePrimaryButtonUIState { button ->
                    button?.copy(enabled = false)
                }
            },
            enabled = enabled,
            lockVisible = isCompleteFlow,
        )
    }
}

internal fun USBankAccountFormArguments.updateMandateText(
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

    onMandateTextChanged(updatedText, false)
}
