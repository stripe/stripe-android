package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.CashAppPayRequirement
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CashAppPayMandateTextSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object CashAppPayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.CashAppPay

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        val requiresMandate = metadata.hasIntentToSetup()

        val localLayoutSpecs = if (requiresMandate) {
            listOf(CashAppPayMandateTextSpec())
        } else {
            emptyList()
        }

        return SupportedPaymentMethod(
            code = "cashapp",
            requiresMandate = requiresMandate,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_cashapp,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_cash_app_pay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            requirement = CashAppPayRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields + localLayoutSpecs),
        )
    }
}
