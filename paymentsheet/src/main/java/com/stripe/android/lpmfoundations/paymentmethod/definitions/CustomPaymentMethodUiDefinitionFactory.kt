package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.DisplayableCustomPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.IdentifierSpec

internal class CustomPaymentMethodUiDefinitionFactory(
    private val displayableCustomPaymentMethod: DisplayableCustomPaymentMethod
) : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = displayableCustomPaymentMethod.id,
            displayName = displayableCustomPaymentMethod.displayName.resolvableString,
            subtitle = displayableCustomPaymentMethod.subtitle,
            lightThemeIconUrl = displayableCustomPaymentMethod.logoUrl,
            darkThemeIconUrl = displayableCustomPaymentMethod.logoUrl,
            iconResource = 0,
            iconResourceNight = 0,
            iconRequiresTinting = false,
        )
    }

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        displayableCustomPaymentMethod.subtitle?.let { subtitle ->
            builder.header(
                StaticTextElement(
                    identifier = IdentifierSpec.Generic("CustomPaymentMethodHeader"),
                    text = subtitle,
                )
            )
        }

        if (displayableCustomPaymentMethod.doesNotCollectBillingDetails) {
            builder.ignoreContactInformationRequirements()
            builder.ignoreBillingAddressRequirements()
        }
    }
}
