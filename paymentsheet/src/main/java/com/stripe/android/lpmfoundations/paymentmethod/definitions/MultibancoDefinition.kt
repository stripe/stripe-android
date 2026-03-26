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

internal object MultibancoDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Multibanco

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = MultibancoUiDefinitionFactory
}

private object MultibancoUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = MultibancoDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_multibanco,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_multibanco_day,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_multibanco_night,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder
    ) {
        builder.requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
    }
}
