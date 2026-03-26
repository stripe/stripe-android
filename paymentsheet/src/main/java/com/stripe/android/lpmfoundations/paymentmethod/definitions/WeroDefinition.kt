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
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement

internal object WeroDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Wero

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = WeroUiDefinitionFactory
}

private object WeroUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = WeroDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_wero,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_wero,
        iconResourceNight = null,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder
            .element(
                formElement = SectionElement.wrap(
                    sectionFieldElement = CountryElement(
                        identifier = IdentifierSpec.Country,
                        controller = DropdownFieldController(
                            config = CountryConfig(
                                onlyShowCountryCodes = setOf("DE", "BE", "FR"),
                            ),
                            initialValue = arguments.initialValues[IdentifierSpec.Country],
                        )
                    )
                )
            )
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
    }
}
