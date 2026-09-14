package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.FormElementsBuilder
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.BacsDebitAccountNumberConfig
import com.stripe.android.ui.core.elements.BacsDebitSortCodeConfig
import com.stripe.android.uicore.elements.CheckboxFieldController
import com.stripe.android.uicore.elements.CheckboxFieldElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController

internal object BacsDebitDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.BacsDebit

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = true

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = BacsDebitUiDefinitionFactory
}

private object BacsDebitUiDefinitionFactory : UiDefinitionFactory.Simple() {
    private val sortCodeIdentifier = FormFieldId.Generic("bacs_debit[sort_code]")
    private val accountNumberIdentifier = FormFieldId.Generic("bacs_debit[account_number]")

    override fun createSupportedPaymentMethod(
        metadata: PaymentMethodMetadata,
    ) = SupportedPaymentMethod(
        paymentMethodDefinition = BacsDebitDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_bacs_debit,
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
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .element(
                SectionElement.wrap(
                    sectionFieldElements = listOf(
                        SimpleTextElement(
                            identifier = sortCodeIdentifier,
                            controller = SimpleTextFieldController(
                                textFieldConfig = BacsDebitSortCodeConfig(),
                                initialValue = arguments.initialValues[sortCodeIdentifier],
                            ),
                        ),
                        SimpleTextElement(
                            identifier = accountNumberIdentifier,
                            controller = SimpleTextFieldController(
                                textFieldConfig = BacsDebitAccountNumberConfig(),
                                initialValue = arguments.initialValues[accountNumberIdentifier],
                            ),
                        ),
                    ),
                    label = resolvableString(R.string.stripe_bacs_bank_account_title),
                )
            )
            .apply {
                if (!arguments.requiresBillingAddressForAutomaticTax) {
                    requireBillingAddressIfAllowed()
                }

                if (metadata.mandateAllowed(BacsDebitDefinition.type)) {
                    footer(
                        CheckboxFieldElement(
                            identifier = FormFieldId.BacsDebitConfirmed,
                            controller = CheckboxFieldController(
                                labelResource = CheckboxFieldController.LabelResource(
                                    R.string.stripe_bacs_confirm_mandate_label,
                                    arguments.merchantName,
                                ),
                                debugTag = "BACS_MANDATE_CHECKBOX",
                                initialValue = arguments.initialValues[FormFieldId.BacsDebitConfirmed].toBoolean(),
                            ),
                        )
                    )
                }
            }
    }
}
