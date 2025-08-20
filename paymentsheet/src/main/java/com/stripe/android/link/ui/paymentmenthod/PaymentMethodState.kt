package com.stripe.android.link.ui.paymentmenthod

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.uicore.elements.FormElement

internal data class PaymentMethodState(
    val formArguments: FormArguments,
    private val formElements: List<FormElement>,
    val primaryButtonState: PrimaryButtonState,
    val primaryButtonLabel: ResolvableString,
    private val isValidating: Boolean,
    val paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    val errorMessage: ResolvableString? = null
) {
    val formUiElements: List<FormElement> = formElements.onEach { element ->
        element.onValidationStateChanged(isValidating)
    }
}
