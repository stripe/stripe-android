package com.stripe.android.link.ui.cardedit

import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.uicore.elements.FormElement

@Immutable
internal data class CardEditState(
    val paymentDetailsId: String,
    val isProcessing: Boolean,
    val isDefault: Boolean,
    val setAsDefault: Boolean,
    val errorMessage: ResolvableString?,
    val linkAccount: LinkAccount,
    val formData: FormData? = null,
)

@Immutable
internal data class FormData(
    val formElements: List<FormElement>,
    val formArguments: FormArguments,
    val usBankAccountFormArguments: USBankAccountFormArguments,
    val onFormFieldValuesChanged: (FormFieldValues?) -> Unit,
)
