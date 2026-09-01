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
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextSpec
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.TranslationId

internal object AuBecsDebitDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.AuBecsDebit

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = true

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = AuBecsDebitUiDefinitionFactory
}

private object AuBecsDebitUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = AuBecsDebitDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_au_becs_debit,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
        iconResourceNight = null,
        outlinedIconResource = R.drawable.stripe_ic_paymentsheet_pm_bank_outlined,
        iconRequiresTinting = true,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .overrideContactInformationPosition(
                type = ContactInformationCollectionMode.Name,
                formElement = NameSpec(
                    labelTranslationId = TranslationId.AuBecsAccountName,
                ).transform(arguments.initialValues),
            )
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .element(BsbSpec().transform(arguments.initialValues))
            .element(AuBankAccountNumberSpec().transform(arguments.initialValues))
            .apply {
                if (metadata.mandateAllowed(AuBecsDebitDefinition.type)) {
                    footer(AuBecsDebitMandateTextSpec().transform(arguments.merchantName))
                }
            }
    }
}
