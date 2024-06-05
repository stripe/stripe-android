package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.dimensionResource
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.FormElement
import com.stripe.android.paymentsheet.ui.LinkElement
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun VerticalModeFormUI(interactor: VerticalModeFormInteractor) {
    val horizontalPadding = dimensionResource(
        id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )

    var hasSentInteractionEvent by remember { mutableStateOf(false) }
    val state by interactor.state.collectAsState()

    FormElement(
        enabled = !state.isProcessing,
        selectedPaymentMethodCode = state.selectedPaymentMethodCode,
        formElements = state.formElements,
        formArguments = state.formArguments,
        usBankAccountFormArguments = state.usBankAccountFormArguments,
        horizontalPadding = horizontalPadding,
        onFormFieldValuesChanged = { formValues ->
            interactor.handleViewAction(
                VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged(formValues)
            )
        },
        onInteractionEvent = {
            if (!hasSentInteractionEvent) {
                interactor.handleViewAction(VerticalModeFormInteractor.ViewAction.FieldInteraction)
                hasSentInteractionEvent = true
            }
        },
    )

    LinkElement(
        linkConfigurationCoordinator = state.linkConfigurationCoordinator,
        linkSignupMode = state.linkSignupMode,
        enabled = !state.isProcessing,
        horizontalPadding = horizontalPadding,
        onLinkSignupStateChanged = {
            interactor.handleViewAction(VerticalModeFormInteractor.ViewAction.LinkSignupStateChanged(it))
        },
    )
}
