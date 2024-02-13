package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.EpsRequirement
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object EpsDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Eps

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun addRequirement(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = "eps",
            requiresMandate = true,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_eps,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_eps,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = EpsRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
    }
}
