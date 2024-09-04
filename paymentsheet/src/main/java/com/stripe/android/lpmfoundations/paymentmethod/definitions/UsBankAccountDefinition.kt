package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.ui.core.R as StripeUiCoreR

internal object UsBankAccountDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.USBankAccount

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.FinancialConnectionsSdk,
        AddPaymentMethodRequirement.ValidUsBankVerificationMethod,
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = true

    override fun uiDefinitionFactory(): UiDefinitionFactory = UsBankAccountUiDefinitionFactory
}

private object UsBankAccountUiDefinitionFactory : UiDefinitionFactory.Simple {
    override fun createSupportedPaymentMethod(): SupportedPaymentMethod = SupportedPaymentMethod(
        code = UsBankAccountDefinition.type.code,
        displayNameResource = StripeUiCoreR.string.stripe_paymentsheet_payment_method_us_bank_account,
        iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_bank,
        iconRequiresTinting = true,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
    )

    override fun createFormHeaderInformation(customerHasSavedPaymentMethods: Boolean): FormHeaderInformation {
        return createSupportedPaymentMethod().asFormHeaderInformation().copy(
            displayName = StripeUiCoreR.string.stripe_paymentsheet_add_us_bank_account.resolvableString,
            shouldShowIcon = false,
        )
    }

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        val defaultName = arguments.initialValues[IdentifierSpec.Name]
        val defaultEmail = arguments.initialValues[IdentifierSpec.Email]

        val headerElement = StaticTextElement(
            identifier = IdentifierSpec.Generic("static_text"),
            stringResId = if (metadata.hasIntentToSetup()) {
                R.string.stripe_paymentsheet_save_bank_title
            } else {
                R.string.stripe_paymentsheet_pay_with_bank_title
            },
        )

        val nameElement = SectionElement.wrap(
            SimpleTextElement(
                identifier = IdentifierSpec.Name,
                controller = NameConfig.createController(defaultName),
            )
        )

        val emailElement = SectionElement.wrap(
            EmailElement(initialValue = defaultEmail)
        )

        return FormElementsBuilder(arguments)
            .header(headerElement)
            .element(nameElement)
            .element(emailElement)
            .build()
    }
}
