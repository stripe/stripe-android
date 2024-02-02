package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.P24Requirement
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object P24Definition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.P24

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun addRequirement(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = "p24",
            requiresMandate = false,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_p24,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_p24,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = P24Requirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
    }
}
