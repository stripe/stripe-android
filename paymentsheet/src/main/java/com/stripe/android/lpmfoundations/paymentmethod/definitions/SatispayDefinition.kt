package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement

internal object SatispayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Satispay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override val supportsTermDisplayConfiguration: Boolean = true

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean {
        return metadata.hasIntentToSetup(type.code) && metadata.mandateAllowed(type)
    }

    override fun uiDefinitionFactory(): UiDefinitionFactory = SatispayUiDefinitionFactory
}

private object SatispayUiDefinitionFactory : UiDefinitionFactory.RequiresSharedDataSpec {
    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = SatispayDefinition,
        sharedDataSpec = sharedDataSpec,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_satispay,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_satispay_day,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_satispay_night,
    )

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
        transformSpecToElements: TransformSpecToElements
    ): List<FormElement> {
        val localLayoutSpecs = if (SatispayDefinition.requiresMandate(metadata)) {
            listOf(MandateTextSpec(stringResId = R.string.stripe_satispay_mandate))
        } else {
            emptyList()
        }
        return transformSpecToElements.transform(
            metadata = metadata,
            specs = sharedDataSpec.fields + localLayoutSpecs,
            termsDisplay = metadata.termsDisplayForType(SatispayDefinition.type),
        )
    }
}
