package com.stripe.android.common.taptoadd.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddCollectionHandler
import com.stripe.android.common.taptoadd.TapToAddCollectionHandler
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultTapToAddCollectingInteractorTest {
    @Test
    fun `create launches collection with payment method metadata`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)

        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.Collected(
                PaymentMethodFactory.card(last4 = "4242")
            ),
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isEqualTo(metadata)
        }
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(isTapToAddSupported = true),
        collectResult: TapToAddCollectionHandler.CollectionState =
            TapToAddCollectionHandler.CollectionState.Collected(PaymentMethodFactory.card(last4 = "4242")),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        FakeTapToAddCollectionHandler.test(collectResult) {
            block(
                Scenario(
                    interactor = DefaultTapToAddCollectingInteractor(
                        paymentMethodMetadata = metadata,
                        coroutineScope = this@runTest,
                        tapToAddCollectionHandler = handler,
                    ),
                    collectionHandlerScenario = this,
                )
            )
        }
    }

    private class Scenario(
        val interactor: TapToAddCollectingInteractor,
        val collectionHandlerScenario: FakeTapToAddCollectionHandler.Scenario,
    )
}
