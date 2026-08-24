package com.stripe.android.paymentsheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.forms.FormFieldValues

internal interface FormHelper : FormDefinitionFactory {

    fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String)

    sealed interface FormType {
        object Empty : FormType
        data class MandateOnly(val mandate: ResolvableString) : FormType
        object UserInteractionRequired : FormType
    }
}
