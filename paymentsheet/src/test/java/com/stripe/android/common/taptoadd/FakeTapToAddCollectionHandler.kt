package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal class FakeTapToAddCollectionHandler private constructor(
    private val collectResult: TapToAddCollectionHandler.CollectionState =
        TapToAddCollectionHandler.CollectionState.Collected,
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
        suspend fun test(
            collectResult: TapToAddCollectionHandler.CollectionState =
                TapToAddCollectionHandler.CollectionState.Collected,
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
                TapToAddCollectionHandler.CollectionState.Collected,
        ) = FakeTapToAddCollectionHandler(collectResult)
    }
}
