package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.DisplayableCustomPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

internal class CustomPaymentMethodUiDefinitionFactory(
    private val displayableCustomPaymentMethod: DisplayableCustomPaymentMethod
) : UiDefinitionFactory.Simple {
    override fun createSupportedPaymentMethod(): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = displayableCustomPaymentMethod.id,
            displayName = displayableCustomPaymentMethod.displayName.resolvableString,
            subtitle = displayableCustomPaymentMethod.subcopy?.resolvableString,
            lightThemeIconUrl = displayableCustomPaymentMethod.logoUrl,
            darkThemeIconUrl = displayableCustomPaymentMethod.logoUrl,
            iconResource = 0,
            iconRequiresTinting = false,
        )
    }

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments
    ): List<FormElement> {
        val builder = FormElementsBuilder(arguments)

        displayableCustomPaymentMethod.subcopy?.let { subcopy ->
            builder.header(
                StaticTextElement(
                    identifier = IdentifierSpec.Generic("CustomPaymentMethodHeader"),
                    text = subcopy.resolvableString,
                )
            )
        }

        if (!displayableCustomPaymentMethod.collectsBillingDetails) {
            builder.ignoreBillingAddressRequirements()
        }

        return builder.build()
    }
}
