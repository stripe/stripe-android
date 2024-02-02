package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.IdealRequirement
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.IdentifierSpec

internal object IdealDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Ideal

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun addRequirement(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOfNotNull(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods.takeIf { hasIntentToSetup },
    )

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
    ): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = "ideal",
            requiresMandate = metadata.hasIntentToSetup(),
            displayNameResource = R.string.stripe_paymentsheet_payment_method_ideal,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_ideal,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = IdealRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields),
            placeholderOverrideList = if (metadata.hasIntentToSetup()) {
                listOf(IdentifierSpec.Name, IdentifierSpec.Email)
            } else {
                emptyList()
            }
        )
    }
}
