package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.uicore.elements.FormElement

internal abstract class FakeFormHelper : FormHelper {
    override fun formElementsForCode(code: String): List<FormElement> {
        TODO("Not yet implemented")
    }

    override fun createFormArguments(paymentMethodCode: PaymentMethodCode): FormArguments {
        TODO("Not yet implemented")
    }

    override fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String) {
        TODO("Not yet implemented")
    }

    override fun requiresFormScreen(selectedPaymentMethodCode: String): Boolean {
        TODO("Not yet implemented")
    }
}
