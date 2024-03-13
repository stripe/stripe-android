package com.stripe.android.lpmfoundations.paymentmethod.definitions

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

internal object AmazonPayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.AmazonPay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        val requiresMandate = metadata.hasIntentToSetup()

        val localLayoutSpecs: List<FormItemSpec> = if (requiresMandate) {
            listOf(MandateTextSpec(stringResId = R.string.stripe_amazon_pay_mandate))
        } else {
            emptyList()
        }

        return SupportedPaymentMethod(
            code = "amazon_pay",
            requiresMandate = requiresMandate,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_amazon_pay,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_amazon_pay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
            formSpec = LayoutSpec(sharedDataSpec.fields + localLayoutSpecs),
        )
    }
}
