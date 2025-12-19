package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
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

internal object FpxDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Fpx

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = FpxUiDefinitionFactory
}

private object FpxUiDefinitionFactory : UiDefinitionFactory.Simple() {
    private val fpsIdentifier = IdentifierSpec.Generic("fpx[bank]")

    override fun createSupportedPaymentMethod() = SupportedPaymentMethod(
        paymentMethodDefinition = FpxDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_fpx,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_fpx,
        iconResourceNight = null,
        iconRequiresTinting = false,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder
    ) {
        builder
            .element(
                formElement = SectionElement.wrap(
                    sectionFieldElement = SimpleDropdownElement(
                        identifier = fpsIdentifier,
                        controller = DropdownFieldController(
                            config = SimpleDropdownConfig(
                                label = R.string.stripe_fpx_bank.resolvableString,
                                items = items,
                            ),
                            initialValue = arguments.initialValues[fpsIdentifier],
                        ),
                    ),
                ),
            )
    }

    private val items = listOf(
        DropdownItemSpec(apiValue = "affin_bank", displayText = "Affin Bank"),
        DropdownItemSpec(apiValue = "alliance_bank", displayText = "Alliance Bank"),
        DropdownItemSpec(apiValue = "ambank", displayText = "AmBank"),
        DropdownItemSpec(apiValue = "bank_islam", displayText = "Bank Islam"),
        DropdownItemSpec(apiValue = "bank_muamalat", displayText = "Bank Muamalat"),
        DropdownItemSpec(apiValue = "bank_rakyat", displayText = "Bank Rakyat"),
        DropdownItemSpec(apiValue = "bsn", displayText = "BSN"),
        DropdownItemSpec(apiValue = "cimb", displayText = "CIMB Clicks"),
        DropdownItemSpec(apiValue = "hong_leong_bank", displayText = "Hong Leong Bank"),
        DropdownItemSpec(apiValue = "hsbc", displayText = "HSBC BANK"),
        DropdownItemSpec(apiValue = "kfh", displayText = "KFH"),
        DropdownItemSpec(apiValue = "maybank2e", displayText = "Maybank2E"),
        DropdownItemSpec(apiValue = "maybank2u", displayText = "Maybank2U"),
        DropdownItemSpec(apiValue = "ocbc", displayText = "OCBC Bank"),
        DropdownItemSpec(apiValue = "public_bank", displayText = "Public Bank"),
        DropdownItemSpec(apiValue = "rhb", displayText = "RHB Bank"),
        DropdownItemSpec(apiValue = "standard_chartered", displayText = "Standard Chartered"),
        DropdownItemSpec(apiValue = "uob", displayText = "UOB Bank"),
    )
}
