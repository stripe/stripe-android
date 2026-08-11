package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.luxe.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.PaymentMethodMessageHeaderElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

internal object KlarnaDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Klarna

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean {
        return metadata.hasIntentToSetup(type.code) && metadata.mandateAllowed(type)
    }

    override fun uiDefinitionFactory(metadata: PaymentMethodMetadata): UiDefinitionFactory {
        return if (FeatureFlags.enableKlarnaFormRemoval.isEnabled) {
            KlarnaRemovedFormUiDefinitionFactory
        } else {
            KlarnaUiDefinitionFactory
        }
    }
}

private object KlarnaUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = KlarnaDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_klarna,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
        iconResourceNight = null,
        subtitle = R.string.stripe_klarna_pay_later.resolvableString
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder
            .header(formElement = getKlarnaHeader(arguments, metadata))
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .countryOnly(
                allowedCountryCodes =
                    metadata.billingDetailsCollectionConfiguration.allowedBillingCountries,
                initialValue = arguments.initialValues[IdentifierSpec.Country],
            )
            .apply {
                if (KlarnaDefinition.requiresMandate(metadata)) {
                    builder.footer(
                        formElement = MandateTextElement(
                            stringResId = R.string.stripe_klarna_mandate,
                            args = listOf(arguments.merchantName, arguments.merchantName)
                        )
                    )
                }
            }
    }

    private fun getKlarnaHeader(
        arguments: UiDefinitionFactory.Arguments,
        metadata: PaymentMethodMetadata
    ): FormElement {
        val message = arguments.paymentMethodMessagingPromotionsHelper?.getPromotionIfAvailableForCode(
            PaymentMethod.Type.Klarna.code,
            metadata
        )
        return if (message != null) {
            PaymentMethodMessageHeaderElement(
                identifier = IdentifierSpec.Generic("klarna_promotion"),
                promotion = message
            )
        } else {
            StaticTextElement(
                identifier = IdentifierSpec.Generic("klarna_header_text"),
                stringResId = R.string.stripe_klarna_buy_now_pay_later
            )
        }
    }
}

private object KlarnaRemovedFormUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata) = SupportedPaymentMethod(
        paymentMethodDefinition = KlarnaDefinition,
        displayNameResource = R.string.stripe_paymentsheet_payment_method_klarna,
        iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
        iconResourceNight = null,
        subtitle = R.string.stripe_klarna_pay_later.resolvableString
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder
    ) {
        if (metadata.stripeIntent is SetupIntent) {
            if (arguments.billingDetailsCollectionConfiguration.address !=
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            ) {
                builder.header(
                    StaticTextElement(
                        IdentifierSpec.Generic("klarna_header_text"),
                        stringResId = R.string.stripe_klarna_buy_now_pay_later
                    )
                )
            }
            builder.countryOnly(
                allowedCountryCodes = arguments.billingDetailsCollectionConfiguration.allowedBillingCountries,
                initialValue = metadata.stripeIntent.countryCode,
            )
        }

        if (KlarnaDefinition.requiresMandate(metadata)) {
            builder.footer(
                formElement = MandateTextElement(
                    stringResId = R.string.stripe_klarna_mandate,
                    args = listOf(arguments.merchantName, arguments.merchantName)
                )
            )
        }
    }
}
