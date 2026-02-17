package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.common.taptoadd.FakeTapToAddCollectionHandler
import com.stripe.android.common.taptoadd.TapToAddCollectionHandler
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultTapToAddCollectingInteractorTest {
    @Test
    fun `create launches collection with payment method metadata`() {
        val metadata = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)

        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.Collected(
                PaymentMethodFactory.card(last4 = "4242")
            ),
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isEqualTo(metadata)
        }
    }

    @Test
    fun `onCollected is invoked when collection succeeds`() {
        val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.Collected(paymentMethod),
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isNotNull()
            assertThat(onCollected.awaitItem()).isEqualTo(paymentMethod)
        }
    }

    @Test
    fun `onFailedCollection is invoked with displayMessage when collection fails with display message`() {
        val errorMessage = "Connection failed".resolvableString

        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = RuntimeException("underlying"),
                displayMessage = errorMessage,
            ),
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isNotNull()
            assertThat(onFailedCollection.awaitItem()).isEqualTo(errorMessage)
        }
    }

    @Test
    fun `onFailedCollection is invoked with stripe error message when collection fails without display message`() {
        val exception = RuntimeException("underlying")

        runScenario(
            collectResult = TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = exception,
                displayMessage = null,
            ),
        ) {
            assertThat(collectionHandlerScenario.collectCalls.awaitItem()).isNotNull()
            assertThat(onFailedCollection.awaitItem()).isEqualTo(exception.stripeErrorMessage())
        }
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(isTapToAddSupported = true),
        collectResult: TapToAddCollectionHandler.CollectionState =
            TapToAddCollectionHandler.CollectionState.Collected(PaymentMethodFactory.card(last4 = "4242")),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val testScope = this

        val onCollected = Turbine<PaymentMethod>()
        val onFailedCollection = Turbine<ResolvableString>()

        FakeTapToAddCollectionHandler.test(collectResult) {
            val scenario = Scenario(
                interactor = DefaultTapToAddCollectingInteractor(
                    paymentMethodMetadata = metadata,
                    coroutineScope = testScope,
                    tapToAddCollectionHandler = handler,
                    onCollected = { onCollected.add(it) },
                    onFailedCollection = { onFailedCollection.add(it) },
                ),
                onCollected = onCollected,
                onFailedCollection = onFailedCollection,
                collectionHandlerScenario = this,
            )
            testScope.advanceUntilIdle()
            block(scenario)
        }
    }

    private class Scenario(
        val interactor: TapToAddCollectingInteractor,
        val onCollected: ReceiveTurbine<PaymentMethod>,
        val onFailedCollection: ReceiveTurbine<ResolvableString>,
        val collectionHandlerScenario: FakeTapToAddCollectionHandler.Scenario,
    )
}
