package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.KlarnaMandateTextSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement

internal object KlarnaDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Klarna

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = metadata.hasIntentToSetup()

    override fun uiDefinitionFactory(): UiDefinitionFactory = KlarnaUiDefinitionFactory
}

private object KlarnaUiDefinitionFactory : UiDefinitionFactory.RequiresSharedDataSpec {
    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = KlarnaDefinition,
        sharedDataSpec = sharedDataSpec,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_klarna,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
        subtitle = R.string.stripe_klarna_pay_later.resolvableString
    )

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
        transformSpecToElements: TransformSpecToElements
    ): List<FormElement> {
        val localLayoutSpecs = if (KlarnaDefinition.requiresMandate(metadata)) {
            listOf(KlarnaMandateTextSpec())
        } else {
            emptyList()
        }
        return transformSpecToElements.transform(
            metadata = metadata,
            specs = sharedDataSpec.fields + localLayoutSpecs
        )
    }
}
