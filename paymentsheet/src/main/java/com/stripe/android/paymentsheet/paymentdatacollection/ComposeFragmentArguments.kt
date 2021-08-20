package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import com.stripe.android.paymentsheet.forms.FormFieldValues
import kotlinx.parcelize.Parcelize

@Parcelize
data class ComposeFragmentArguments(
    val supportedPaymentMethodName: String,
    val saveForFutureUseInitialVisibility: Boolean,
    val saveForFutureUseInitialValue: Boolean,
    val merchantName: String
) : Parcelable

fun ComposeFragmentArguments.toFormFieldValues(): FormFieldValues {
    return FormFieldValues(
        emptyMap(),
        saveForFutureUse = this.saveForFutureUseInitialValue,
        showsMandate = false
    )
}
