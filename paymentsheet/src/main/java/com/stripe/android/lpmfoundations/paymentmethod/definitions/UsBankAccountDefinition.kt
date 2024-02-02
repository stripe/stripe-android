package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.USBankAccountRequirement
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object UsBankAccountDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.USBankAccount

    override val supportedAsSavedPaymentMethod: Boolean = true

    override fun addRequirement(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.FinancialConnectionsSdk,
        AddPaymentMethodRequirement.ValidUsBankVerificationMethod,
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec
    ): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = "us_bank_account",
            requiresMandate = true,
            displayNameResource = R.string.stripe_paymentsheet_payment_method_us_bank_account,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = true,
            requirement = USBankAccountRequirement,
            formSpec = LayoutSpec(sharedDataSpec.fields)
        )
    }
}
