package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object PayNowDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.PayNow

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf()

    override fun uiDefinitionFactory(): UiDefinitionFactory = PayNowUiDefinitionFactory
}

private object PayNowUiDefinitionFactory : UiDefinitionFactory.RequiresSharedDataSpec {
    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = PayNowDefinition,
        sharedDataSpec = sharedDataSpec,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_paynow,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_paynow_day,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_paynow_night,
    )
}