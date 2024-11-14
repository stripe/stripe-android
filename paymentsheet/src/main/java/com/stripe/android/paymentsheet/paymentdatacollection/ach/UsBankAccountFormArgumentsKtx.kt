package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.R as StripeUiCoreR

internal fun USBankAccountFormArguments.handleScreenStateChanged(
    screenState: BankFormScreenState,
    enabled: Boolean,
    onPrimaryButtonClick: () -> Unit,
) {
    screenState.error?.let {
        onError(it)
    }

    if (screenState.linkedBankAccount == null) {
        updatePrimaryButton(
            text = resolvableString(StripeUiCoreR.string.stripe_continue_button_label),
            onClick = onPrimaryButtonClick,
            enabled = enabled,
            shouldShowProcessingWhenClicked = isCompleteFlow,
        )
    } else {
        // Clear the primary button
        onUpdatePrimaryButtonUIState { null }
    }

    onMandateTextChanged(screenState.linkedBankAccount?.mandateText, false)
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
