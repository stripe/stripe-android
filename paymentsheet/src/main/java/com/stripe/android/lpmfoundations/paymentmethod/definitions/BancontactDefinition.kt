package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.BancontactRequirement
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.IdentifierSpec

internal object BancontactDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Bancontact

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = "bancontact",
            requiresMandate = metadata.hasIntentToSetup(),
            displayNameResource = R.string.stripe_paymentsheet_payment_method_bancontact,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bancontact,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = BancontactRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields),
            placeholderOverrideList = if (metadata.hasIntentToSetup()) {
                listOf(IdentifierSpec.Name, IdentifierSpec.Email)
            } else {
                emptyList()
            }
        )
    }
}
