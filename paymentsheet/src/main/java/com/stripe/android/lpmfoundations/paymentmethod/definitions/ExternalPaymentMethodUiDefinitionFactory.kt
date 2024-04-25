package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.uicore.elements.FormElement

internal class ExternalPaymentMethodUiDefinitionFactory(
    private val externalPaymentMethodSpec: ExternalPaymentMethodSpec
) : UiDefinitionFactory.Simple {
    override fun createSupportedPaymentMethod(): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = externalPaymentMethodSpec.type,
            displayName = resolvableString(externalPaymentMethodSpec.label),
            lightThemeIconUrl = externalPaymentMethodSpec.lightImageUrl,
            darkThemeIconUrl = externalPaymentMethodSpec.darkImageUrl,
            iconResource = 0,
            tintIconOnSelection = false,
        )
    }

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments
    ): List<FormElement> {
        return FormElementsBuilder(arguments).build()
    }
}
