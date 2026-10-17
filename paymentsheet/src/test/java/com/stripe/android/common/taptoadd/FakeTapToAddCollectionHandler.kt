package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal class FakeTapToAddCollectionHandler private constructor(
    private val collectResult: TapToAddCollectionHandler.CollectionState,
    private val continueController: Deferred<Unit> = CompletableDeferred(Unit),
) : TapToAddCollectionHandler {
    private val collectCalls = Turbine<PaymentMethodMetadata>()

    override suspend fun collect(metadata: PaymentMethodMetadata): TapToAddCollectionHandler.CollectionState {
        collectCalls.add(metadata)

        continueController.await()

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
            continueController: Deferred<Unit> = CompletableDeferred(Unit),
            block: suspend Scenario.() -> Unit,
        ) {
            val handler = FakeTapToAddCollectionHandler(collectResult, continueController)

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
