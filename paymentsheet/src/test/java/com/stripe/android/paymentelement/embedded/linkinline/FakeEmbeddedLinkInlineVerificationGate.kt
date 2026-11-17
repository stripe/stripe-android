package com.stripe.android.paymentelement.embedded.linkinline

import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal class FakeEmbeddedLinkInlineVerificationGate(
    var isEligible: Boolean = false,
) : EmbeddedLinkInlineVerificationGate {
    val isEligibleCalls = Turbine<IsEligibleCall>()

    override fun isEligible(
        metadata: PaymentMethodMetadata,
        customer: CustomerState?,
        integrationConfiguration: PaymentElementLoader.Configuration,
    ): Boolean {
        isEligibleCalls.add(IsEligibleCall(metadata, customer, integrationConfiguration))
        return isEligible
    }

    fun ensureAllEventsConsumed() {
        isEligibleCalls.ensureAllEventsConsumed()
    }

    data class IsEligibleCall(
        val metadata: PaymentMethodMetadata,
        val customer: CustomerState?,
        val integrationConfiguration: PaymentElementLoader.Configuration,
    )
}
