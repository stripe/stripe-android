package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec

internal class ExternalPaymentMethodUiDefinitionFactory(
    private val externalPaymentMethodSpec: ExternalPaymentMethodSpec
) : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod(metadata: PaymentMethodMetadata): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = externalPaymentMethodSpec.type,
            displayName = externalPaymentMethodSpec.label.resolvableString,
            lightThemeIconUrl = externalPaymentMethodSpec.lightImageUrl,
            darkThemeIconUrl = externalPaymentMethodSpec.darkImageUrl,
            iconResource = 0,
            iconResourceNight = 0,
            iconRequiresTinting = false,
        )
    }
}
