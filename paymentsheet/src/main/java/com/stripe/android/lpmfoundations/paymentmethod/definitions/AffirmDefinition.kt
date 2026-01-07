package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.elements.AffirmHeaderElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as UiCoreR

internal object AffirmDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Affirm

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = false

    override fun uiDefinitionFactory(): UiDefinitionFactory = AffirmUiDefinitionFactory
}

private object AffirmUiDefinitionFactory : UiDefinitionFactory.Simple() {
    override fun createSupportedPaymentMethod() = SupportedPaymentMethod(
        code = PaymentMethod.Type.Affirm.code,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
        displayNameResource = UiCoreR.string.stripe_paymentsheet_payment_method_affirm,
        iconResource = UiCoreR.drawable.stripe_ic_paymentsheet_pm_affirm,
        iconResourceNight = null,
        subtitle = StripeR.string.stripe_affirm_buy_now_pay_later_plaintext.resolvableString
    )

    override fun buildFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
        builder: FormElementsBuilder,
    ) {
        builder.header(AffirmHeaderElement(identifier = IdentifierSpec.Generic("affirm_header")))
    }
}
