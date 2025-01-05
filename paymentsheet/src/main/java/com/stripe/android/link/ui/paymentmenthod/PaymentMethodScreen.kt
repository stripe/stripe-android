package com.stripe.android.link.ui.paymentmenthod

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.FormElement
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun PaymentMethodScreen(
    viewModel: PaymentMethodViewModel
) {
    val state by viewModel.state.collectAsState()

    FormElement(
        enabled = true,
        selectedPaymentMethodCode = state.selectedPaymentMethodCode,
        formElements = state.formElements,
        formArguments = state.formArguments,
        usBankAccountFormArguments = state.usBankAccountFormArguments,
        horizontalPadding = 16.dp,
        onFormFieldValuesChanged = { formValues ->
//            interactor.handleViewAction(
//                VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged(formValues)
//            )
        },
        onInteractionEvent = {
//            if (!hasSentInteractionEvent) {
//                interactor.handleViewAction(VerticalModeFormInteractor.ViewAction.FieldInteraction)
//                hasSentInteractionEvent = true
//            }
        },
    )
}
