package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed interface NewOrExternalPaymentSelection {

    val paymentSelection: PaymentSelection

    fun getPaymentMethodCode(): PaymentMethodCode

    fun getType(): String

    fun getPaymentMethodCreateParams(): PaymentMethodCreateParams?

    fun getPaymentMethodExtraParams(): PaymentMethodExtraParams?

    data class New(override val paymentSelection: PaymentSelection.New) : NewOrExternalPaymentSelection {

        override fun getPaymentMethodCode(): PaymentMethodCode {
            return when (paymentSelection) {
                is PaymentSelection.New.LinkInline -> PaymentMethod.Type.Card.code
                is PaymentSelection.New.Card,
                is PaymentSelection.New.USBankAccount,
                is PaymentSelection.New.GenericPaymentMethod -> paymentSelection.paymentMethodCreateParams.typeCode
            }
        }

        override fun getType(): String = paymentSelection.paymentMethodCreateParams.typeCode

        override fun getPaymentMethodCreateParams(): PaymentMethodCreateParams =
            paymentSelection.paymentMethodCreateParams

        override fun getPaymentMethodExtraParams(): PaymentMethodExtraParams? =
            paymentSelection.paymentMethodExtraParams
    }

    data class External(override val paymentSelection: PaymentSelection.ExternalPaymentMethod) :
        NewOrExternalPaymentSelection {

        override fun getPaymentMethodCode(): PaymentMethodCode = paymentSelection.type

        override fun getType(): String = paymentSelection.type

        override fun getPaymentMethodCreateParams(): PaymentMethodCreateParams? = null

        override fun getPaymentMethodExtraParams(): PaymentMethodExtraParams? = null
    }
}
