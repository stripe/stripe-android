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

internal object NgBankTransferDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.NgBankTransfer

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(AddPaymentMethodRequirement.UnsupportedForSetup)

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = NgBankTransferUiDefinitionFactory
}

private object NgBankTransferUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = NgBankTransferDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_ng_bank_transfer,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_ng_bank_transfer,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_ng_bank_transfer_night,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        if (metadata.mandateAllowed(PaymentMethod.Type.NgBankTransfer)) {
            builder.footer(
                MandateTextElement(
                    stringResId = R.string.stripe_ng_payment_mor_notice,
                    args = listOf(NIGERIAN_PAYMENT_METHOD_TERMS_URL),
                )
            )
        }
    }
}
