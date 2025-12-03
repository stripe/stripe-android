package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object PayPayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.PayPay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> {
        return if (FeatureFlags.enablePayPay.isEnabled) {
            // We haven't implemented support for PayPay for Setup, because PayPay is not currently eligible for setup.
            setOf(AddPaymentMethodRequirement.UnsupportedForSetup)
        } else {
            setOf(AddPaymentMethodRequirement.Unsupported)
        }
    }

    override fun uiDefinitionFactory(): UiDefinitionFactory = PayPayUiDefinitionFactory
}

private object PayPayUiDefinitionFactory : UiDefinitionFactory.RequiresSharedDataSpec {
    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = PayPayDefinition,
        sharedDataSpec = sharedDataSpec,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_paypay,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_promptpay_day,
        iconResourceNight = null,
    )
}
