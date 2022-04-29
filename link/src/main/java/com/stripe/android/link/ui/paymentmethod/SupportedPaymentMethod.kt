package com.stripe.android.link.ui.paymentmethod

import android.os.Parcelable
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.ui.core.forms.LinkCardForm
import kotlinx.parcelize.Parcelize

internal sealed class SupportedPaymentMethod(
    val type: PaymentMethod.Type,
    /**
     * This describes how the UI should look.
     */
    val formSpec: LayoutSpec,
) : Parcelable {

    /**
     * Builds the [ConsumerPaymentDetailsCreateParams] used to create this payment method.
     */
    abstract fun createParams(paymentMethodCreateParams: PaymentMethodCreateParams):
        ConsumerPaymentDetailsCreateParams

    /**
     * Creates a map containing additional parameters that must be sent during payment confirmation.
     */
    open fun extraConfirmationParams(formValues: Map<IdentifierSpec, FormFieldEntry>):
        Map<String, Any>? = null

    @Parcelize
    class Card : SupportedPaymentMethod(
        PaymentMethod.Type.Card,
        LinkCardForm
    ) {
        override fun createParams(paymentMethodCreateParams: PaymentMethodCreateParams) =
            ConsumerPaymentDetailsCreateParams.Card(paymentMethodCreateParams.toParamMap())

        override fun extraConfirmationParams(formValues: Map<IdentifierSpec, FormFieldEntry>) =
            mapOf("card" to mapOf("cvc" to formValues[IdentifierSpec.CardCvc]?.value))
    }
}
