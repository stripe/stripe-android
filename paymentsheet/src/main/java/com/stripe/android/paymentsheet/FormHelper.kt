package com.stripe.android.paymentsheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.uicore.elements.FormElement

internal interface FormHelper {

    fun formElementsForCode(code: String): List<FormElement>

    fun createFormArguments(
        paymentMethodCode: PaymentMethodCode,
    ): FormArguments

    fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String)

    fun getPaymentMethodParams(
        formValues: FormFieldValues?,
        selectedPaymentMethodCode: String
    ): PaymentMethodCreateParams?

    fun formTypeForCode(paymentMethodCode: PaymentMethodCode): FormType

    /**
     * Synchronously builds a PaymentSelection from the given form values and payment method code.
     * This bypasses the async flow collection chain to ensure we get the latest state immediately.
     */
    fun buildPaymentSelection(
        formValues: FormFieldValues?,
        selectedPaymentMethodCode: String
    ): PaymentSelection?

    sealed interface FormType {
        object Empty : FormType
        data class MandateOnly(val mandate: ResolvableString) : FormType
        object UserInteractionRequired : FormType
    }
}
