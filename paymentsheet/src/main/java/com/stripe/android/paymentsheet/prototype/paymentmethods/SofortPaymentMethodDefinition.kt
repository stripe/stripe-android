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

internal object SofortPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.Sofort

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods,
    )

    override suspend fun initialAddState(
        metadata: ParsingMetadata
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        uiDefinition {
            selector {
                displayName = resolvableString(R.string.stripe_paymentsheet_payment_method_sofort)
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_klarna
            }

            if (requireBillingAddressCollection) {
//                element(SofortCountryElementDefinition())
            } else {
                requireBillingAddress(
                    availableCountries = setOf("AT", "BE", "DE", "ES", "IT", "NL"),
                )
            }
        }
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        // TODO: Get country from billing address collection info.
        // TODO: Put country into sofort[country] rather than the default.
        return PaymentMethodConfirmParams(
            PaymentMethodCreateParams.create(PaymentMethodCreateParams.Sofort("US"))
        )
    }
}
