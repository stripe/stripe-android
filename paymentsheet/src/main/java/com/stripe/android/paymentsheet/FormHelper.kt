package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.uicore.elements.FormElement

internal interface FormHelper {

    fun formElementsForCode(code: String): List<FormElement>

    fun createFormArguments(
        paymentMethodCode: PaymentMethodCode,
    ): FormArguments

    fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String)

    fun getPaymentMethodParams(formValues: FormFieldValues?, selectedPaymentMethodCode: String): PaymentMethodParams?

    fun requiresFormScreen(selectedPaymentMethodCode: String): Boolean

    data class PaymentMethodParams(
        val paymentMethodCreateParams: PaymentMethodCreateParams,
    )
}
