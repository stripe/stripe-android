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
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SimpleDropdownConfig
import com.stripe.android.ui.core.elements.SimpleDropdownElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionElement

internal object NaverPayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.NaverPay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean {
        return metadata.hasIntentToSetup(type.code) && metadata.mandateAllowed(type)
    }

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = NaverPayUiDefinitionFactory
}

private object NaverPayUiDefinitionFactory : UiDefinitionFactory.Simple() {
    private val fundingIdentifier = FormFieldId.Generic("naver_pay[funding]")

    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = NaverPayDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_naver_pay,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_naver_pay,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_naver_pay_night,
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder.element(
            SectionElement.wrap(
                SimpleDropdownElement(
                    identifier = fundingIdentifier,
                    controller = DropdownFieldController(
                        config = SimpleDropdownConfig(
                            label = R.string.stripe_naver_pay_funding_label.resolvableString,
                            items = fundingItems,
                        ),
                        initialValue = arguments.initialValues[fundingIdentifier],
                    ),
                ),
            ),
        )

        if (NaverPayDefinition.requiresMandate(metadata)) {
            builder.footer(
                MandateTextElement(
                    stringResId = R.string.stripe_kr_card_mandate,
                    args = listOf(arguments.merchantName),
                )
            )
        }
    }

    private val fundingItems = listOf(
        DropdownItem(
            apiValue = "card",
            displayText = "Naver Pay Card",
        ),
        DropdownItem(
            apiValue = "points",
            displayText = "Naver Pay Money/Point",
        ),
    )
}
