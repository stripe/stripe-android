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

internal object EpsDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Eps

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = EpsUiDefinitionFactory
}

private object EpsUiDefinitionFactory : UiDefinitionFactory.Simple() {
    private val epsIdentifier = IdentifierSpec.Generic("eps[bank]")

    override fun createSupportedPaymentMethod() = SupportedPaymentMethod(
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
        DropdownItemSpec(
            apiValue = "arzte_und_apotheker_bank",
            displayText = "Ärzte- und Apothekerbank",
        ),
        DropdownItemSpec(
            apiValue = "austrian_anadi_bank_ag",
            displayText = "Austrian Anadi Bank AG",
        ),
        DropdownItemSpec(
            apiValue = "bank_austria",
            displayText = "Bank Austria",
        ),
        DropdownItemSpec(
            apiValue = "brull_kallmus_bank_ag",
            displayText = "bank99 AG",
        ),
        DropdownItemSpec(
            apiValue = "bankhaus_carl_spangler",
            displayText = "Bankhaus Carl Spängler & Co.AG",
        ),
        DropdownItemSpec(
            apiValue = "bankhaus_schelhammer_und_schattera_ag",
            displayText = "Bankhaus Schelhammer & Schattera AG",
        ),
        DropdownItemSpec(
            apiValue = "bawag_psk_ag",
            displayText = "BAWAG P.S.K. AG",
        ),
        DropdownItemSpec(
            apiValue = "bks_bank_ag",
            displayText = "BKS Bank AG",
        ),
        DropdownItemSpec(
            apiValue = "btv_vier_lander_bank",
            displayText = "BTV VIER LÄNDER BANK",
        ),
        DropdownItemSpec(
            apiValue = "capital_bank_grawe_gruppe_ag",
            displayText = "Capital Bank Grawe Gruppe AG",
        ),
        DropdownItemSpec(
            apiValue = "dolomitenbank",
            displayText = "Dolomitenbank",
        ),
        DropdownItemSpec(
            apiValue = "easybank_ag",
            displayText = "Easybank AG",
        ),
        DropdownItemSpec(
            apiValue = "erste_bank_und_sparkassen",
            displayText = "Erste Bank und Sparkassen",
        ),
        DropdownItemSpec(
            apiValue = "hypo_alpeadriabank_international_ag",
            displayText = "Hypo Alpe-Adria-Bank International AG",
        ),
        DropdownItemSpec(
            apiValue = "hypo_noe_lb_fur_niederosterreich_u_wien",
            displayText = "HYPO NOE LB für Niederösterreich u. Wien",
        ),
        DropdownItemSpec(
            apiValue = "hypo_oberosterreich_salzburg_steiermark",
            displayText = "HYPO Oberösterreich,Salzburg,Steiermark",
        ),
        DropdownItemSpec(
            apiValue = "hypo_tirol_bank_ag",
            displayText = "Hypo Tirol Bank AG",
        ),
        DropdownItemSpec(
            apiValue = "hypo_vorarlberg_bank_ag",
            displayText = "Hypo Vorarlberg Bank AG",
        ),
        DropdownItemSpec(
            apiValue = "hypo_bank_burgenland_aktiengesellschaft",
            displayText = "HYPO-BANK BURGENLAND Aktiengesellschaft",
        ),
        DropdownItemSpec(
            apiValue = "marchfelder_bank",
            displayText = "Marchfelder Bank",
        ),
        DropdownItemSpec(
            apiValue = "oberbank_ag",
            displayText = "Oberbank AG",
        ),
        DropdownItemSpec(
            apiValue = "raiffeisen_bankengruppe_osterreich",
            displayText = "Raiffeisen Bankengruppe Österreich",
        ),
        DropdownItemSpec(
            apiValue = "schoellerbank_ag",
            displayText = "Schoellerbank AG",
        ),
        DropdownItemSpec(
            apiValue = "sparda_bank_wien",
            displayText = "Sparda-Bank Wien",
        ),
        DropdownItemSpec(
            apiValue = "volksbank_gruppe",
            displayText = "Volksbank Gruppe",
        ),
        DropdownItemSpec(
            apiValue = "volkskreditbank_ag",
            displayText = "Volkskreditbank AG",
        ),
        DropdownItemSpec(
            apiValue = "vr_bank_braunau",
            displayText = "VR-Bank Braunau",
        ),
    )
}
