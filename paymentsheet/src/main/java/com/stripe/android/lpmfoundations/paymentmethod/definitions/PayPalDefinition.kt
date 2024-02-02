package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.PaypalRequirement
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object PayPalDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.PayPal

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun addRequirement(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf()

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        val localLayoutSpecs: List<FormItemSpec> = if (metadata.hasIntentToSetup()) {
            listOf(MandateTextSpec(stringResId = R.string.stripe_paypal_mandate))
        } else {
            emptyList()
        }

        return SupportedPaymentMethod(
            code = "paypal",
            requiresMandate = metadata.hasIntentToSetup(),
            displayNameResource = R.string.stripe_paymentsheet_payment_method_paypal,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_paypal,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = PaypalRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields + localLayoutSpecs)
        )
    }
}
