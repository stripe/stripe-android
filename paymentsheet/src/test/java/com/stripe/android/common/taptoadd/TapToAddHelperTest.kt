package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TapToAddHelperTest {
    @Test
    fun `create returns null when isTapToAddSupported is false`() = runTest {
        val helper = TapToAddHelper.create(
            coroutineScope = CoroutineScope(coroutineContext),
            tapToAddCollectionHandler = FakeTapToAddCollectionHandler.noOp(),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                isTapToAddSupported = false,
            ),
            onCollectingUpdated = {},
            onError = {},
        )

        assertThat(helper).isNull()
    }

    @Test
    fun `create returns TapToAddHelper when isTapToAddSupported is true`() = runTest {
        val helper = TapToAddHelper.create(
            coroutineScope = CoroutineScope(coroutineContext),
            tapToAddCollectionHandler = FakeTapToAddCollectionHandler.noOp(),
            paymentMethodMetadata = DEFAULT_METADATA,
            onCollectingUpdated = {},
            onError = {},
        )

        assertThat(helper).isNotNull()
    }

    @Test
    fun `collect sets processing to true at start and false at end on success`() = runScenario(
        metadata = DEFAULT_METADATA,
        collectResult = TapToAddCollectionHandler.CollectionState.Collected,
    ) {
        helper.startPaymentMethodCollection()

        assertThat(updateProcessingCalls.awaitItem()).isTrue()
        assertThat(handlerScenario.collectCalls.awaitItem()).isEqualTo(DEFAULT_METADATA)
        assertThat(updateProcessingCalls.awaitItem()).isFalse()
    }

    @Test
    fun `collect sets processing to true at start and false at end on failure and updates error`() {
        val error = IllegalStateException()
        val message = "Failed".resolvableString

        runScenario(
            metadata = DEFAULT_METADATA,
            collectResult = TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = error,
                displayMessage = message,
            ),
        ) {
            helper.startPaymentMethodCollection()

            assertThat(updateProcessingCalls.awaitItem()).isTrue()
            assertThat(handlerScenario.collectCalls.awaitItem()).isEqualTo(DEFAULT_METADATA)
            assertThat(updateProcessingCalls.awaitItem()).isFalse()
            assertThat(updateErrorCalls.awaitItem()).isEqualTo(message)
        }
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = DEFAULT_METADATA,
        collectResult: TapToAddCollectionHandler.CollectionState =
            TapToAddCollectionHandler.CollectionState.Collected,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val updateProcessingCalls = Turbine<Boolean>()
        val updateErrorCalls = Turbine<ResolvableString?>()

        FakeTapToAddCollectionHandler.test(collectResult) {
            block(
                Scenario(
                    helper = createTapToAddHelper(
                        collectionHandler = handler,
                        metadata = metadata,
                        updateProcessing = {
                            updateProcessingCalls.add(it)
                        },
                        updateError = {
                            updateErrorCalls.add(it)
                        },
                    ),
                    handlerScenario = this,
                    updateProcessingCalls = updateProcessingCalls,
                    updateErrorCalls = updateErrorCalls,
                )
            )
        }

        updateProcessingCalls.ensureAllEventsConsumed()
        updateErrorCalls.ensureAllEventsConsumed()
    }

    private suspend fun createTapToAddHelper(
        collectionHandler: TapToAddCollectionHandler,
        metadata: PaymentMethodMetadata,
        updateProcessing: (Boolean) -> Unit,
        updateError: (ResolvableString?) -> Unit,
    ): TapToAddHelper {
        return DefaultTapToAddHelper(
            coroutineScope = CoroutineScope(currentCoroutineContext()),
            tapToAddCollectionHandler = collectionHandler,
            paymentMethodMetadata = metadata,
            onCollectingUpdated = updateProcessing,
            onError = updateError,
        )
    }

    private class Scenario(
        val helper: TapToAddHelper,
        val handlerScenario: FakeTapToAddCollectionHandler.Scenario,
        val updateProcessingCalls: ReceiveTurbine<Boolean>,
        val updateErrorCalls: ReceiveTurbine<ResolvableString?>
    )

    private companion object {
        val DEFAULT_METADATA = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)
    }
}
