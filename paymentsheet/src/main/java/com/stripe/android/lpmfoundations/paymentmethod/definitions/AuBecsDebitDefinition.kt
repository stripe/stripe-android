package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AuBankAccountNumberConfig
import com.stripe.android.ui.core.elements.AuBecsDebitMandateTextElement
import com.stripe.android.ui.core.elements.BsbElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.R as StripeR

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
    private val bsbNumberIdentifier = IdentifierSpec.Generic("au_becs_debit[bsb_number]")
    private val accountNumberIdentifier = IdentifierSpec.Generic("au_becs_debit[account_number]")

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
                formElement = SectionElement.wrap(
                    SimpleTextElement(
                        identifier = IdentifierSpec.Name,
                        controller = SimpleTextFieldController(
                            textFieldConfig = SimpleTextFieldConfig(
                                label = StripeR.string.stripe_au_becs_account_name.resolvableString,
                                capitalization = KeyboardCapitalization.Words,
                                keyboard = KeyboardType.Text,
                                optional = false,
                            ),
                            initialValue = arguments.initialValues[IdentifierSpec.Name],
                        ),
                    ),
                ),
            )
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .element(
                BsbElement(
                    identifierSpec = bsbNumberIdentifier,
                    initialValue = arguments.initialValues[bsbNumberIdentifier],
                )
            )
            .element(
                SectionElement.wrap(
                    SimpleTextElement(
                        identifier = accountNumberIdentifier,
                        controller = SimpleTextFieldController(
                            textFieldConfig = AuBankAccountNumberConfig(),
                            initialValue = arguments.initialValues[accountNumberIdentifier],
                        ),
                    ),
                )
            )
            .apply {
                if (metadata.mandateAllowed(AuBecsDebitDefinition.type)) {
                    footer(
                        AuBecsDebitMandateTextElement(
                            identifier = IdentifierSpec.Generic("au_becs_mandate"),
                            merchantName = arguments.merchantName,
                        )
                    )
                }
            }
    }
}
