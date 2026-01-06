package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.testing.PaymentMethodFactory

internal class FakeTapToAddCollectionHandler private constructor(
    private val collectResult: TapToAddCollectionHandler.CollectionState,
) : TapToAddCollectionHandler {
    private val collectCalls = Turbine<PaymentMethodMetadata>()

    override suspend fun collect(metadata: PaymentMethodMetadata): TapToAddCollectionHandler.CollectionState {
        collectCalls.add(metadata)

        return collectResult
    }

    class Scenario(
        val handler: TapToAddCollectionHandler,
        val collectCalls: ReceiveTurbine<PaymentMethodMetadata>,
    )

    companion object {
        private val DEFAULT_PAYMENT_METHOD = PaymentMethodFactory.card(last4 = "4242")

        suspend fun test(
            collectResult: TapToAddCollectionHandler.CollectionState =
                TapToAddCollectionHandler.CollectionState.Collected(DEFAULT_PAYMENT_METHOD),
            block: suspend Scenario.() -> Unit,
        ) {
            val handler = FakeTapToAddCollectionHandler(collectResult)

            block(
                Scenario(
                    handler = handler,
                    collectCalls = handler.collectCalls,
                )
            )

            handler.collectCalls.ensureAllEventsConsumed()
        }

        fun noOp(
            collectResult: TapToAddCollectionHandler.CollectionState =
                TapToAddCollectionHandler.CollectionState.Collected(DEFAULT_PAYMENT_METHOD),
        ) = FakeTapToAddCollectionHandler(collectResult)
    }
}
