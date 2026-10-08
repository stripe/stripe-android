package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.FormElementsBuilder
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId

internal object NgCardDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.NgCard

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(AddPaymentMethodRequirement.UnsupportedForSetup)

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(
        metadata: PaymentMethodMetadata
    ): UiDefinitionFactory = NgCardUiDefinitionFactory
}

private object NgCardUiDefinitionFactory : UiDefinitionFactory.Custom {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = NgCardDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_ng_card,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_ng_card,
        iconResourceNight = R.drawable.stripe_ic_paymentsheet_pm_ng_card_night,
    )

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        val initialValues = arguments.initialValues
        val allowedCountries = arguments.billingDetailsCollectionConfiguration.allowedBillingCountries
        val defaultCountry = "NG".takeIf { allowedCountries.isEmpty() || it in allowedCountries }
        val resolvedArguments = if (initialValues[FormFieldId.Country].isNullOrBlank() && defaultCountry != null) {
            arguments.copy(initialValues = initialValues + (FormFieldId.Country to defaultCountry))
        } else {
            arguments
        }
        return FormElementsBuilder(
            arguments = resolvedArguments,
            supportsAutomaticTaxBillingAddress = true,
        ).footer(
            MandateTextElement(
                stringResId = R.string.stripe_ng_payment_mor_notice,
                args = listOf(NIGERIAN_PAYMENT_METHOD_TERMS_URL),
            )
        ).build()
    }
}
