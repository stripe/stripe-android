package com.stripe.android.link.ui.paymentmethod

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.link.TestFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.uicore.elements.FormElement

internal class PaymentMethodFormHelper : FormHelper {
    var paymentMethodCreateParams: PaymentMethodCreateParams? = PaymentMethodCreateParamsFixtures.DEFAULT_CARD
    val getPaymentMethodParamsCalls = arrayListOf<GetPaymentMethodParamsCall>()

    private val _formFieldValuesChangedCall = Turbine<String>()
    val formFieldValuesChangedCall: ReceiveTurbine<String> = _formFieldValuesChangedCall

    override fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String) {
        _formFieldValuesChangedCall.add(selectedPaymentMethodCode)
    }

    override fun getPaymentMethodParams(
        formValues: FormFieldValues?,
        selectedPaymentMethodCode: String
    ): PaymentMethodCreateParams? {
        require(selectedPaymentMethodCode == PaymentMethod.Type.Card.code) {
            "$selectedPaymentMethodCode payment not supported"
        }
        getPaymentMethodParamsCalls.add(
            GetPaymentMethodParamsCall(
                formValues = formValues,
                selectedPaymentMethodCode = selectedPaymentMethodCode
            )
        )
        return paymentMethodCreateParams
    }

    override fun formElementsForCode(code: String): List<FormElement> {
        require(code == PaymentMethod.Type.Card.code) {
            "$code payment not supported"
        }
        return TestFactory.CARD_FORM_ELEMENTS
    }

    override fun createFormArguments(paymentMethodCode: PaymentMethodCode): FormArguments {
        require(paymentMethodCode == PaymentMethod.Type.Card.code) {
            "$paymentMethodCode payment not supported"
        }
        return TestFactory.CARD_FORM_ARGS
    }

    override fun formTypeForCode(paymentMethodCode: PaymentMethodCode): FormHelper.FormType {
        TODO("Not yet implemented")
    }

    data class GetPaymentMethodParamsCall(
        val formValues: FormFieldValues?,
        val selectedPaymentMethodCode: String
    )
}
