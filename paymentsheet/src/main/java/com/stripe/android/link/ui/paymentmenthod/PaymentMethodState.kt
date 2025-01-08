package com.stripe.android.link.ui.paymentmenthod

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.uicore.elements.FormElement

internal data class PaymentMethodState(
    val isProcessing: Boolean,
    val formArguments: FormArguments,
    val formElements: List<FormElement>,
    val primaryButtonState: PrimaryButtonState,
    val primaryButtonLabel: ResolvableString,
    val paymentSelection: PaymentSelection? = null
)
