package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.KonbiniRequirement
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object KonbiniDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Konbini

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun addRequirement(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = "konbini",
            requiresMandate = false,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_konbini,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_konbini,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = KonbiniRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields),
        )
    }
}
