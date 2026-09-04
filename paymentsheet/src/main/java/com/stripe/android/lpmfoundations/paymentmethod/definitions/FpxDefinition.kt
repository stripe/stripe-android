package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
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

internal object FpxDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Fpx

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = FpxUiDefinitionFactory
}

private object FpxUiDefinitionFactory : UiDefinitionFactory.Simple() {
    private val fpsIdentifier = FormFieldId.Generic("fpx[bank]")

    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
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
        DropdownItem(apiValue = "affin_bank", displayText = "Affin Bank"),
        DropdownItem(apiValue = "agrobank", displayText = "Agrobank"),
        DropdownItem(apiValue = "alliance_bank", displayText = "Alliance Bank"),
        DropdownItem(apiValue = "ambank", displayText = "AmBank"),
        DropdownItem(apiValue = "bank_islam", displayText = "Bank Islam"),
        DropdownItem(apiValue = "bank_muamalat", displayText = "Bank Muamalat"),
        DropdownItem(apiValue = "bank_of_china", displayText = "Bank of China"),
        DropdownItem(apiValue = "bank_rakyat", displayText = "Bank Rakyat"),
        DropdownItem(apiValue = "bsn", displayText = "BSN"),
        DropdownItem(apiValue = "cimb", displayText = "CIMB Clicks"),
        DropdownItem(apiValue = "hong_leong_bank", displayText = "Hong Leong Bank"),
        DropdownItem(apiValue = "hsbc", displayText = "HSBC Bank"),
        DropdownItem(apiValue = "kfh", displayText = "KFH"),
        DropdownItem(apiValue = "maybank2e", displayText = "Maybank2E"),
        DropdownItem(apiValue = "maybank2u", displayText = "Maybank2U"),
        DropdownItem(apiValue = "mbsb_bank", displayText = "MBSB Bank"),
        DropdownItem(apiValue = "ocbc", displayText = "OCBC Bank"),
        DropdownItem(apiValue = "public_bank", displayText = "Public Bank"),
        DropdownItem(apiValue = "rhb", displayText = "RHB Bank"),
        DropdownItem(apiValue = "standard_chartered", displayText = "Standard Chartered"),
        DropdownItem(apiValue = "uob", displayText = "UOB Bank"),
    )
}
