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
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

internal object EpsDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Eps

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = EpsUiDefinitionFactory
}

private object EpsUiDefinitionFactory : UiDefinitionFactory.Simple() {
    private val epsIdentifier = IdentifierSpec.Generic("eps[bank]")

    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = EpsDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_eps,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_eps,
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
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .element(
                formElement = SectionElement.wrap(
                    sectionFieldElement = SimpleDropdownElement(
                        identifier = epsIdentifier,
                        controller = DropdownFieldController(
                            config = SimpleDropdownConfig(
                                label = R.string.stripe_eps_bank.resolvableString,
                                items = items,
                            ),
                            initialValue = arguments.initialValues[epsIdentifier],
                        ),
                    ),
                ),
            )
    }

    private val items = listOf(
        DropdownItem(
            apiValue = "arzte_und_apotheker_bank",
            displayText = "Ärzte- und Apothekerbank",
        ),
        DropdownItem(
            apiValue = "austrian_anadi_bank_ag",
            displayText = "Austrian Anadi Bank AG",
        ),
        DropdownItem(
            apiValue = "bank_austria",
            displayText = "Bank Austria",
        ),
        DropdownItem(
            apiValue = "brull_kallmus_bank_ag",
            displayText = "bank99 AG",
        ),
        DropdownItem(
            apiValue = "bankhaus_carl_spangler",
            displayText = "Bankhaus Carl Spängler & Co.AG",
        ),
        DropdownItem(
            apiValue = "bankhaus_schelhammer_und_schattera_ag",
            displayText = "Bankhaus Schelhammer & Schattera AG",
        ),
        DropdownItem(
            apiValue = "bawag_psk_ag",
            displayText = "BAWAG P.S.K. AG",
        ),
        DropdownItem(
            apiValue = "bks_bank_ag",
            displayText = "BKS Bank AG",
        ),
        DropdownItem(
            apiValue = "btv_vier_lander_bank",
            displayText = "BTV VIER LÄNDER BANK",
        ),
        DropdownItem(
            apiValue = "capital_bank_grawe_gruppe_ag",
            displayText = "Capital Bank Grawe Gruppe AG",
        ),
        DropdownItem(
            apiValue = "dolomitenbank",
            displayText = "Dolomitenbank",
        ),
        DropdownItem(
            apiValue = "easybank_ag",
            displayText = "Easybank AG",
        ),
        DropdownItem(
            apiValue = "erste_bank_und_sparkassen",
            displayText = "Erste Bank und Sparkassen",
        ),
        DropdownItem(
            apiValue = "hypo_alpeadriabank_international_ag",
            displayText = "Hypo Alpe-Adria-Bank International AG",
        ),
        DropdownItem(
            apiValue = "hypo_noe_lb_fur_niederosterreich_u_wien",
            displayText = "HYPO NOE LB für Niederösterreich u. Wien",
        ),
        DropdownItem(
            apiValue = "hypo_oberosterreich_salzburg_steiermark",
            displayText = "HYPO Oberösterreich,Salzburg,Steiermark",
        ),
        DropdownItem(
            apiValue = "hypo_tirol_bank_ag",
            displayText = "Hypo Tirol Bank AG",
        ),
        DropdownItem(
            apiValue = "hypo_vorarlberg_bank_ag",
            displayText = "Hypo Vorarlberg Bank AG",
        ),
        DropdownItem(
            apiValue = "hypo_bank_burgenland_aktiengesellschaft",
            displayText = "HYPO-BANK BURGENLAND Aktiengesellschaft",
        ),
        DropdownItem(
            apiValue = "marchfelder_bank",
            displayText = "Marchfelder Bank",
        ),
        DropdownItem(
            apiValue = "oberbank_ag",
            displayText = "Oberbank AG",
        ),
        DropdownItem(
            apiValue = "raiffeisen_bankengruppe_osterreich",
            displayText = "Raiffeisen Bankengruppe Österreich",
        ),
        DropdownItem(
            apiValue = "schoellerbank_ag",
            displayText = "Schoellerbank AG",
        ),
        DropdownItem(
            apiValue = "sparda_bank_wien",
            displayText = "Sparda-Bank Wien",
        ),
        DropdownItem(
            apiValue = "volksbank_gruppe",
            displayText = "Volksbank Gruppe",
        ),
        DropdownItem(
            apiValue = "volkskreditbank_ag",
            displayText = "Volkskreditbank AG",
        ),
        DropdownItem(
            apiValue = "vr_bank_braunau",
            displayText = "VR-Bank Braunau",
        ),
    )
}
