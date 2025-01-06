package com.stripe.android.link.ui.paymentmenthod

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.paymentsheet.model.label
import com.stripe.android.paymentsheet.ui.FormElement
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun PaymentMethodScreen(
    viewModel: PaymentMethodViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column {
        FormElement(
            enabled = true,
            selectedPaymentMethodCode = state.selectedPaymentMethodCode,
            formElements = state.formElements,
            formArguments = state.formArguments,
            usBankAccountFormArguments = state.usBankAccountFormArguments,
            horizontalPadding = 16.dp,
            onFormFieldValuesChanged = { formValues ->
                viewModel.formValuesChanged(formValues)
            },
            onInteractionEvent = {},
        )

        Text(state.paymentSelection?.label?.resolve(context) ?: "null")

        PrimaryButton(
            label = state.primaryButtonLabel.resolve(context),
            state = state.primaryButtonState,
            onButtonClick = {

            }
        )
    }
}
