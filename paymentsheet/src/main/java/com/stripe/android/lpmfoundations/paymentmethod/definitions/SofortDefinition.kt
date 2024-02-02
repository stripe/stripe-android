package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SofortRequirement
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.IdentifierSpec

internal object SofortDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Sofort

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun addRequirement(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = "sofort",
            requiresMandate = metadata.hasIntentToSetup(),
            displayNameResource = R.string.stripe_paymentsheet_payment_method_sofort,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = SofortRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields),
            placeholderOverrideList = if (metadata.hasIntentToSetup()) {
                listOf(IdentifierSpec.Name, IdentifierSpec.Email)
            } else {
                emptyList()
            }
        )
    }
}
