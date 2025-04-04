package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed interface NewPaymentOptionSelection {

    val paymentSelection: PaymentSelection

    fun getPaymentMethodCode(): PaymentMethodCode

    fun getType(): String

    fun getPaymentMethodCreateParams(): PaymentMethodCreateParams?

    fun getPaymentMethodExtraParams(): PaymentMethodExtraParams?

    fun getPaymentMethodOptionParams(): PaymentMethodOptionsParams?

    data class New(override val paymentSelection: PaymentSelection.New) : NewPaymentOptionSelection {
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

        override fun getPaymentMethodOptionParams(): PaymentMethodOptionsParams? =
            paymentSelection.paymentMethodOptionsParams
    }

    data class External(override val paymentSelection: PaymentSelection.ExternalPaymentMethod) :
        NewPaymentOptionSelection {

        override fun getPaymentMethodCode(): PaymentMethodCode = paymentSelection.type

        override fun getType(): String = paymentSelection.type

        override fun getPaymentMethodCreateParams(): PaymentMethodCreateParams? = null

        override fun getPaymentMethodExtraParams(): PaymentMethodExtraParams? = null

        override fun getPaymentMethodOptionParams(): PaymentMethodOptionsParams? = null
    }

    data class Custom(override val paymentSelection: PaymentSelection.CustomPaymentMethod) :
        NewPaymentOptionSelection {

        override fun getPaymentMethodCode(): PaymentMethodCode = paymentSelection.id

        override fun getType(): String = paymentSelection.id

        override fun getPaymentMethodCreateParams(): PaymentMethodCreateParams? = null

        override fun getPaymentMethodExtraParams(): PaymentMethodExtraParams? = null

        override fun getPaymentMethodOptionParams(): PaymentMethodOptionsParams? = null
    }
}
