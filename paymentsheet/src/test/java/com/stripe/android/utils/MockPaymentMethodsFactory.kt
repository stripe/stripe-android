package com.stripe.android.utils

import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.forms.resources.LpmRepository

object MockPaymentMethodsFactory {

    fun create(): List<LpmRepository.SupportedPaymentMethod> {
        return listOf(
            mockPaymentMethod(
                code = "card",
                displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
                tintIconOnSelection = true
            ),
            mockPaymentMethod(
                code = "sofort",
                displayNameResource = R.string.stripe_paymentsheet_payment_method_sofort,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna
            ),
            mockPaymentMethod(
                code = "klarna",
                displayNameResource = R.string.stripe_paymentsheet_payment_method_klarna,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna
            ),
            mockPaymentMethod(
                code = "paypal",
                displayNameResource = R.string.stripe_paymentsheet_payment_method_paypal,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_paypal
            )
        )
    }

    private fun mockPaymentMethod(
        code: String,
        displayNameResource: Int,
        iconResource: Int,
        tintIconOnSelection: Boolean = false
    ): LpmRepository.SupportedPaymentMethod {
        return LpmRepository.SupportedPaymentMethod(
            code = code,
            requiresMandate = false,
            displayNameResource = displayNameResource,
            iconResource = iconResource,
            tintIconOnSelection = tintIconOnSelection,
            requirement = PaymentMethodRequirements(
                piRequirements = emptySet(),
                siRequirements = null,
                confirmPMFromCustomer = false
            ),
            formSpec = LayoutSpec(emptyList())
        )
    }
}
