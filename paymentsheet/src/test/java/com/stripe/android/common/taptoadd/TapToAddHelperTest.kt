package com.stripe.android.common.taptoadd

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TapToAddHelperTest {

    @Test
    fun `hasPreviouslyAttemptedCollection is initially false`() = runScenario {
        assertThat(helper.hasPreviouslyAttemptedCollection).isFalse()
    }

    @Test
    fun `hasPreviouslyAttemptedCollection is true after startPaymentMethodCollection`() = runScenario {
        helper.startPaymentMethodCollection(DEFAULT_METADATA)

        assertThat(helper.hasPreviouslyAttemptedCollection).isTrue()
    }

    @Test
    fun `hasPreviouslyAttemptedCollection remains true after multiple start calls`() = runScenario {
        helper.startPaymentMethodCollection(DEFAULT_METADATA)
        helper.startPaymentMethodCollection(DEFAULT_METADATA)
        helper.startPaymentMethodCollection(DEFAULT_METADATA)

        assertThat(helper.hasPreviouslyAttemptedCollection).isTrue()
    }

    @Test
    fun `startPaymentMethodCollection calls tap to add collect`() = runScenario {
        helper.startPaymentMethodCollection(DEFAULT_METADATA)

        assertThat(handlerScenario.collectCalls.awaitItem()).isEqualTo(DEFAULT_METADATA)
    }

    @Test
    fun `hasPreviouslyAttemptedCollection persists across instances via SavedStateHandle`() = runTest {
        val savedStateHandle = SavedStateHandle()

        FakeTapToAddCollectionHandler.test {
            val helper1 = DefaultTapToAddHelper(
                coroutineScope = CoroutineScope(currentCoroutineContext()),
                tapToAddCollectionHandler = handler,
                savedStateHandle = savedStateHandle,
            )

            assertThat(helper1.hasPreviouslyAttemptedCollection).isFalse()
            helper1.startPaymentMethodCollection(DEFAULT_METADATA)
            assertThat(helper1.hasPreviouslyAttemptedCollection).isTrue()

            val helper2 = DefaultTapToAddHelper(
                coroutineScope = CoroutineScope(currentCoroutineContext()),
                tapToAddCollectionHandler = handler,
                savedStateHandle = savedStateHandle,
            )

            assertThat(helper2.hasPreviouslyAttemptedCollection).isTrue()
        }
    }

    private fun runScenario(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        FakeTapToAddCollectionHandler.test(
            collectResult = TapToAddCollectionHandler.CollectionState.Collected(
                PaymentMethodFactory.card(last4 = "4242")
            )
        ) {
            block(
                Scenario(
                    helper = createTapToAddHelper(
                        collectionHandler = handler,
                        savedStateHandle = savedStateHandle,
                    ),
                    handlerScenario = this,
                )
            )
        }
    }

    private suspend fun createTapToAddHelper(
        collectionHandler: TapToAddCollectionHandler,
        savedStateHandle: SavedStateHandle,
    ): TapToAddHelper {
        return DefaultTapToAddHelper(
            coroutineScope = CoroutineScope(currentCoroutineContext()),
            tapToAddCollectionHandler = collectionHandler,
            savedStateHandle = savedStateHandle,
        )
    }

    private class Scenario(
        val helper: TapToAddHelper,
        val handlerScenario: FakeTapToAddCollectionHandler.Scenario,
    )

    private companion object {
        val DEFAULT_METADATA = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)
    }
}
