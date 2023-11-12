package com.stripe.android.paymentsheet.prototype.paymentmethods

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.prototype.AddPaymentMethodRequirement
import com.stripe.android.paymentsheet.prototype.InitialAddPaymentMethodState
import com.stripe.android.paymentsheet.prototype.ParsingMetadata
import com.stripe.android.paymentsheet.prototype.PaymentMethodConfirmParams
import com.stripe.android.paymentsheet.prototype.PaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.UiState
import com.stripe.android.paymentsheet.prototype.buildInitialState
import com.stripe.android.ui.core.R

internal object RevolutPayPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.RevolutPay

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = emptySet()

    override suspend fun initialAddState(
        metadata: ParsingMetadata,
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        uiDefinition {
            selector {
                displayName = resolvableString(R.string.stripe_paymentsheet_payment_method_revolut_pay)
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_revolut_pay
            }

            if (metadata.hasIntentToSetup()) {
                mandate(resolvableString(R.string.stripe_revolut_mandate, metadata.merchantName))
            }
        }
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        return PaymentMethodConfirmParams(
            PaymentMethodCreateParams.createRevolutPay()
        )
    }
}
