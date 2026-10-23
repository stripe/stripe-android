package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.FormElementsBuilder
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.PaymentMethodMessageHeaderElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId

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
        builder: FormElementsBuilder,
    ) {
        builder
            .nonInteractiveHeader { arguments ->
                getKlarnaHeader(arguments, metadata)
            }
            .overrideContactInformationPosition(ContactInformationCollectionMode.Name)
            .requireContactInformationIfAllowed(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Email)
            .overrideContactInformationPosition(ContactInformationCollectionMode.Phone)
            .requireCountry(
                allowedCountryCodes = metadata.billingDetailsCollectionConfiguration.allowedBillingCountries,
                initialValue = { arguments -> arguments.initialValues[FormFieldId.Country] },
            )
            .addMandateIfAllowed(
                identifier = FormFieldId.Generic("mandate"),
                stringResId = R.string.stripe_klarna_mandate,
                args = listOf(metadata.merchantName, metadata.merchantName),
                topPadding = 8.dp,
            )
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
                identifier = FormFieldId.Generic("klarna_promotion"),
                promotion = message
            )
        } else {
            StaticTextElement(
                identifier = FormFieldId.Generic("klarna_header_text"),
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
        builder: FormElementsBuilder
    ) {
        if (metadata.stripeIntent is SetupIntent &&
            metadata.billingDetailsCollectionConfiguration.address !=
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
        ) {
            builder
                .nonInteractiveHeader {
                    StaticTextElement(
                        FormFieldId.Generic("klarna_header_text"),
                        stringResId = R.string.stripe_klarna_buy_now_pay_later
                    )
                }
                .requireCountry(
                    allowedCountryCodes = metadata.billingDetailsCollectionConfiguration.allowedBillingCountries,
                    initialValue = { arguments ->
                        arguments.initialValues[FormFieldId.Country] ?: metadata.stripeIntent.countryCode
                    },
                )
        }

        builder.addMandateIfAllowed(
            identifier = FormFieldId.Generic("mandate"),
            stringResId = R.string.stripe_klarna_mandate,
            args = listOf(metadata.merchantName, metadata.merchantName),
            topPadding = 8.dp,
        )
    }
}
