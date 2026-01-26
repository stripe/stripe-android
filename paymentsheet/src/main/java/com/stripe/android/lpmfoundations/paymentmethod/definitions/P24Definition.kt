package com.stripe.android.lpmfoundations.paymentmethod.definitions

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
import com.stripe.android.ui.core.elements.DropdownItemSpec
import com.stripe.android.ui.core.elements.SimpleDropdownConfig
import com.stripe.android.ui.core.elements.SimpleDropdownElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

internal object P24Definition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.P24

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = P24UiDefinitionFactory
}

private object P24UiDefinitionFactory : UiDefinitionFactory.Simple() {
    private val p24BankIdentifier = IdentifierSpec.Generic("p24[bank]")

    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = P24Definition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_p24,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_p24,
        iconResourceNight = null,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder
    ) {
        builder
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Name)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .element(
                formElement = SectionElement.wrap(
                    sectionFieldElement = SimpleDropdownElement(
                        identifier = p24BankIdentifier,
                        controller = DropdownFieldController(
                            config = SimpleDropdownConfig(
                                label = R.string.stripe_p24_bank.resolvableString,
                                items = items,
                            ),
                            initialValue = arguments.initialValues[p24BankIdentifier],
                        ),
                    ),
                ),
            )
    }

    private val items = listOf(
        DropdownItemSpec(displayText = "Alior Bank", apiValue = "alior_bank"),
        DropdownItemSpec(displayText = "Bank Millenium", apiValue = "bank_millennium"),
        DropdownItemSpec(displayText = "Bank Nowy BFG S.A.", apiValue = "bank_nowy_bfg_sa"),
        DropdownItemSpec(displayText = "Bank PEKAO S.A", apiValue = "bank_pekao_sa"),
        DropdownItemSpec(displayText = "Bank spółdzielczy", apiValue = "banki_spbdzielcze"),
        DropdownItemSpec(displayText = "BLIK", apiValue = "blik"),
        DropdownItemSpec(displayText = "BNP Paribas", apiValue = "bnp_paribas"),
        DropdownItemSpec(displayText = "BOZ", apiValue = "boz"),
        DropdownItemSpec(displayText = "CitiHandlowy", apiValue = "citi_handlowy"),
        DropdownItemSpec(displayText = "Credit Agricole", apiValue = "credit_agricole"),
        DropdownItemSpec(displayText = "e-Transfer Pocztowy24", apiValue = "etransfer_pocztowy24"),
        DropdownItemSpec(displayText = "Getin Bank", apiValue = "getin_bank"),
        DropdownItemSpec(displayText = "IdeaBank", apiValue = "ideabank"),
        DropdownItemSpec(displayText = "ING", apiValue = "ing"),
        DropdownItemSpec(displayText = "inteligo", apiValue = "inteligo"),
        DropdownItemSpec(displayText = "mBank", apiValue = "mbank_mtransfer"),
        DropdownItemSpec(displayText = "Nest Przelew", apiValue = "nest_przelew"),
        DropdownItemSpec(displayText = "Noble Pay", apiValue = "noble_pay"),
        DropdownItemSpec(displayText = "Płać z iPKO (PKO BP)", apiValue = "pbac_z_ipko"),
        DropdownItemSpec(displayText = "Plus Bank", apiValue = "plus_bank"),
        DropdownItemSpec(displayText = "Santander", apiValue = "santander_przelew24"),
        DropdownItemSpec(displayText = "Toyota Bank", apiValue = "toyota_bank"),
        DropdownItemSpec(displayText = "VeloBank", apiValue = "velobank"),
        DropdownItemSpec(displayText = "Volkswagen Bank", apiValue = "volkswagen_bank"),
    )
}
