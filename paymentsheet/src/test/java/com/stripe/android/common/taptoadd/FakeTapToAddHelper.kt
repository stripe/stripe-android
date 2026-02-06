package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal class FakeTapToAddHelper private constructor(
    override val hasPreviouslyAttemptedCollection: Boolean = false,
) : TapToAddHelper {
    private val collectCalls = Turbine<PaymentMethodMetadata>()

    override fun startPaymentMethodCollection(paymentMethodMetadata: PaymentMethodMetadata) {
        collectCalls.add(paymentMethodMetadata)
    }

    class Scenario(
        val collectCalls: ReceiveTurbine<PaymentMethodMetadata>,
        val helper: TapToAddHelper,
    )

    companion object {
        suspend fun test(
            hasPreviouslyAttemptedCollection: Boolean = false,
            block: suspend Scenario.() -> Unit,
        ) {
            val helper = FakeTapToAddHelper(hasPreviouslyAttemptedCollection)

            block(
                Scenario(
                    helper = helper,
                    collectCalls = helper.collectCalls,
                )
            )

            helper.collectCalls.ensureAllEventsConsumed()
        }

        fun noOp() = FakeTapToAddHelper()
    }
}
