package com.stripe.android.link.ui.paymentmenthod

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.uicore.elements.FormElement

internal data class PaymentMethodState(
    val isProcessing: Boolean,
    val formArguments: FormArguments,
    val formElements: List<FormElement>,
    val primaryButtonState: PrimaryButtonState,
    val primaryButtonLabel: ResolvableString,
    val paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    val errorMessage: ResolvableString? = null
)
