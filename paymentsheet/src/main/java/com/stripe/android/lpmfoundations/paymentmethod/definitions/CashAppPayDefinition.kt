package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CashAppPayMandateTextSpec
import com.stripe.android.ui.core.elements.SharedDataSpec
import com.stripe.android.uicore.elements.FormElement

internal object CashAppPayDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.CashAppPay

    override val supportedAsSavedPaymentMethod: Boolean = false

    override fun requirementsToBeUsedAsNewPaymentMethod(
        hasIntentToSetup: Boolean
    ): Set<AddPaymentMethodRequirement> = setOf()

    override fun requiresMandate(metadata: PaymentMethodMetadata): Boolean = metadata.hasIntentToSetup()

    override fun supportedPaymentMethod(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
    ): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = "cashapp",
            displayNameResource = R.string.stripe_paymentsheet_payment_method_cashapp,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_cash_app_pay,
            lightThemeIconUrl = sharedDataSpec.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec.selectorIcon?.darkThemePng,
            tintIconOnSelection = false,
        )
    }

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        sharedDataSpec: SharedDataSpec,
        transformSpecToElements: TransformSpecToElements
    ): List<FormElement> {
        val localLayoutSpecs = if (requiresMandate(metadata)) {
            listOf(CashAppPayMandateTextSpec())
        } else {
            emptyList()
        }
        return transformSpecToElements.transform(
            specs = sharedDataSpec.fields + localLayoutSpecs
        )
    }
}
