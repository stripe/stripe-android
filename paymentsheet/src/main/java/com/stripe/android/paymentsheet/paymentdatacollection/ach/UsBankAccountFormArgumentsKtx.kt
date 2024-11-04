package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState.BillingDetailsCollection
import com.stripe.android.paymentsheet.ui.PrimaryButton

internal fun USBankAccountFormArguments.handleScreenStateChanged(
    screenState: USBankAccountFormScreenState,
    enabled: Boolean,
    onPrimaryButtonClick: (USBankAccountFormScreenState) -> Unit,
) {
    screenState.error?.let {
        onError(it)
    }

    if (screenState is BillingDetailsCollection) {
        updatePrimaryButton(
            text = screenState.primaryButtonText,
            onClick = { onPrimaryButtonClick(screenState) },
            enabled = enabled,
            shouldShowProcessingWhenClicked = isCompleteFlow,
        )
    } else {
        // Clear the primary button
        onUpdatePrimaryButtonUIState { null }
    }

    onMandateTextChanged(screenState.mandateText, false)
}

private fun USBankAccountFormArguments.updatePrimaryButton(
    text: ResolvableString,
    onClick: () -> Unit,
    shouldShowProcessingWhenClicked: Boolean,
    enabled: Boolean,
) {
    onUpdatePrimaryButtonUIState {
        PrimaryButton.UIState(
            label = text,
            onClick = {
                if (shouldShowProcessingWhenClicked) {
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
