package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.InitialAddPaymentMethodState
import com.stripe.android.lpmfoundations.PaymentMethodConfirmParams
import com.stripe.android.lpmfoundations.UiState
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.ui.core.R

internal object PayPalPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.PayPal

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = emptySet()

    override suspend fun initialAddState(
        metadata: PaymentMethodMetadata,
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        uiDefinition {
            selector {
                displayName = resolvableString(R.string.stripe_paymentsheet_payment_method_paypal)
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_paypal
            }

            if (metadata.hasIntentToSetup()) {
                mandate(resolvableString(R.string.stripe_paypal_mandate, metadata.merchantName))
            }
        }
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        return PaymentMethodConfirmParams(
            PaymentMethodCreateParams.createPayPal()
        )
    }
}
