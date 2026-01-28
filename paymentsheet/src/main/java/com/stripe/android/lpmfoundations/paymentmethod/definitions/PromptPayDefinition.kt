package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R

internal object PromptPayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.PromptPay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> {
        // We haven't implemented support for PromptPay for Setup, because PromptPay is not currently eligible for
        // setup.
        return setOf(AddPaymentMethodRequirement.UnsupportedForSetup)
    }

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = PromptPayUiDefinitionFactory
}

private object PromptPayUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = PromptPayDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_promptpay,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_promptpay_day,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_promptpay_night,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder
    ) {
        builder
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
    }
}
