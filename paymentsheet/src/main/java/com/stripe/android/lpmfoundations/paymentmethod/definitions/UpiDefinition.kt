package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.UpiElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement

internal object UpiDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Upi

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = UpiUiDefinitionFactory
}

private object UpiUiDefinitionFactory : UiDefinitionFactory.Simple {
    override fun createSupportedPaymentMethod(
        incentive: PaymentMethodIncentive?,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = UpiDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_upi,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_upi,
    )

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments
    ): List<FormElement> {
        val section = SectionElement.wrap(UpiElement(), label = R.string.stripe_paymentsheet_buy_using_upi_id)
        return FormElementsBuilder(arguments).element(section).build()
    }
}
