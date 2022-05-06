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

/**
 * Class representing the Payment Methods that are supported by Link.
 *
 * @param type The Payment Method type
 * @param formSpec Specification of how the payment method data collection UI should look.
 */
internal sealed class SupportedPaymentMethod(
    val type: PaymentMethod.Type,
    val formSpec: LayoutSpec,
) : Parcelable {

    /**
     * Builds the [ConsumerPaymentDetailsCreateParams] used to create this payment method.
     */
    abstract fun createParams(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        email: String
    ): ConsumerPaymentDetailsCreateParams

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
        override fun createParams(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            email: String
        ) = ConsumerPaymentDetailsCreateParams.Card(
            paymentMethodCreateParams.toParamMap(),
            email
        )

        /**
         * CVC is not passed during creation, and must be included when confirming the payment.
         */
        override fun extraConfirmationParams(formValues: Map<IdentifierSpec, FormFieldEntry>) =
            mapOf("card" to mapOf("cvc" to formValues[IdentifierSpec.CardCvc]?.value))
    }
}
