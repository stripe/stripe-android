package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.FormElementsBuilder
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement

internal object NgWalletDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.NgWallet

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(AddPaymentMethodRequirement.UnsupportedForSetup)

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = NgWalletUiDefinitionFactory
}

private object NgWalletUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = NgWalletDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_ng_wallet,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_ng_wallet,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_ng_wallet_night,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        if (metadata.mandateAllowed(PaymentMethod.Type.NgWallet)) {
            builder.footer(
                MandateTextElement(
                    stringResId = R.string.stripe_ng_wallet_mor_notice,
                    args = listOf(NIGERIAN_PAYMENT_METHOD_TERMS_URL),
                )
            )
        }
    }
}
