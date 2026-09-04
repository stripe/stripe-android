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
import com.stripe.android.ui.core.elements.DropdownItem
import com.stripe.android.ui.core.elements.SimpleDropdownConfig
import com.stripe.android.ui.core.elements.SimpleDropdownElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormFieldId
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

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = P24UiDefinitionFactory
}

private object P24UiDefinitionFactory : UiDefinitionFactory.Simple() {
    private val p24BankIdentifier = FormFieldId.Generic("p24[bank]")

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
        DropdownItem(displayText = "Alior Bank", apiValue = "alior_bank"),
        DropdownItem(displayText = "Bank Millenium", apiValue = "bank_millennium"),
        DropdownItem(displayText = "Bank Nowy BFG S.A.", apiValue = "bank_nowy_bfg_sa"),
        DropdownItem(displayText = "Bank PEKAO S.A", apiValue = "bank_pekao_sa"),
        DropdownItem(displayText = "Bank spółdzielczy", apiValue = "banki_spbdzielcze"),
        DropdownItem(displayText = "BLIK", apiValue = "blik"),
        DropdownItem(displayText = "BNP Paribas", apiValue = "bnp_paribas"),
        DropdownItem(displayText = "BOZ", apiValue = "boz"),
        DropdownItem(displayText = "CitiHandlowy", apiValue = "citi_handlowy"),
        DropdownItem(displayText = "Credit Agricole", apiValue = "credit_agricole"),
        DropdownItem(displayText = "e-Transfer Pocztowy24", apiValue = "etransfer_pocztowy24"),
        DropdownItem(displayText = "Getin Bank", apiValue = "getin_bank"),
        DropdownItem(displayText = "IdeaBank", apiValue = "ideabank"),
        DropdownItem(displayText = "ING", apiValue = "ing"),
        DropdownItem(displayText = "inteligo", apiValue = "inteligo"),
        DropdownItem(displayText = "mBank", apiValue = "mbank_mtransfer"),
        DropdownItem(displayText = "Nest Przelew", apiValue = "nest_przelew"),
        DropdownItem(displayText = "Noble Pay", apiValue = "noble_pay"),
        DropdownItem(displayText = "Płać z iPKO (PKO BP)", apiValue = "pbac_z_ipko"),
        DropdownItem(displayText = "Plus Bank", apiValue = "plus_bank"),
        DropdownItem(displayText = "Santander", apiValue = "santander_przelew24"),
        DropdownItem(displayText = "Toyota Bank", apiValue = "toyota_bank"),
        DropdownItem(displayText = "VeloBank", apiValue = "velobank"),
        DropdownItem(displayText = "Volkswagen Bank", apiValue = "volkswagen_bank"),
    )
}
