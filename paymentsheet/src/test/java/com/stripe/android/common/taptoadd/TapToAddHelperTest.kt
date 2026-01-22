package com.stripe.android.common.taptoadd

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentMethodRefresher
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.FakePaymentMethodRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
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
            customerStateHolder = CustomerStateHolder(
                selection = MutableStateFlow(null),
                customerMetadataPermissions = MutableStateFlow(null),
                savedStateHandle = SavedStateHandle(),
            ),
            paymentMethodRefresher = FakePaymentMethodRefresher.noOp(),
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
            customerStateHolder = CustomerStateHolder(
                selection = MutableStateFlow(null),
                customerMetadataPermissions = MutableStateFlow(null),
                savedStateHandle = SavedStateHandle(),
            ),
            paymentMethodRefresher = FakePaymentMethodRefresher.noOp(),
        )

        assertThat(helper).isNotNull()
    }

    @Test
    fun `collectedPaymentMethod is initially null`() = runScenario {
        helper.collectedPaymentMethod.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `collect sets processing to true at start and false at end on success`() = runScenario(
        metadata = DEFAULT_METADATA,
        collectResult = TapToAddCollectionHandler.CollectionState.Collected(PaymentMethodFactory.card(last4 = "4242")),
    ) {
        helper.startPaymentMethodCollection()

        assertThat(updateProcessingCalls.awaitItem()).isTrue()
        assertThat(handlerScenario.collectCalls.awaitItem()).isEqualTo(DEFAULT_METADATA)
        assertThat(updateProcessingCalls.awaitItem()).isFalse()
        assertThat(refresherScenario.refreshCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `collect updates collectedPaymentMethod on success`() {
        val card = PaymentMethodFactory.card(last4 = "4242")

        runScenario(
            metadata = DEFAULT_METADATA,
            collectResult = TapToAddCollectionHandler.CollectionState.Collected(card),
        ) {
            helper.collectedPaymentMethod.test {
                assertThat(awaitItem()).isNull()

                helper.startPaymentMethodCollection()

                assertThat(updateProcessingCalls.awaitItem()).isTrue()
                assertThat(handlerScenario.collectCalls.awaitItem()).isEqualTo(DEFAULT_METADATA)
                assertThat(updateProcessingCalls.awaitItem()).isFalse()

                val collectedPaymentMethod = awaitItem()

                assertThat(collectedPaymentMethod).isNotNull()
                assertThat(collectedPaymentMethod?.paymentMethod).isEqualTo(card)
                assertThat(refresherScenario.refreshCalls.awaitItem()).isNotNull()
            }
        }
    }

    @Test
    fun `collect does not update collectedPaymentMethod on failure`() {
        val error = IllegalStateException()
        val message = "Failed".resolvableString

        runScenario(
            metadata = DEFAULT_METADATA,
            collectResult = TapToAddCollectionHandler.CollectionState.FailedCollection(
                error = error,
                displayMessage = message,
            ),
        ) {
            helper.collectedPaymentMethod.test {
                assertThat(awaitItem()).isNull()

                helper.startPaymentMethodCollection()

                assertThat(updateProcessingCalls.awaitItem()).isTrue()
                assertThat(handlerScenario.collectCalls.awaitItem()).isEqualTo(DEFAULT_METADATA)
                assertThat(updateProcessingCalls.awaitItem()).isFalse()
                assertThat(updateErrorCalls.awaitItem()).isEqualTo(message)

                expectNoEvents()
            }
        }
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

    @Test
    fun `refresh updates payment methods after successful collection`() {
        val paymentMethod = PaymentMethodFactory.card(last4 = "4242")

        runScenario(
            metadata = DEFAULT_METADATA,
            collectResult = TapToAddCollectionHandler.CollectionState.Collected(paymentMethod),
            refreshedPaymentMethods = listOf(paymentMethod),
        ) {
            customerStateHolder.paymentMethods.test {
                assertThat(awaitItem()).isEmpty()

                helper.startPaymentMethodCollection()

                assertThat(updateProcessingCalls.awaitItem()).isTrue()
                assertThat(handlerScenario.collectCalls.awaitItem()).isEqualTo(DEFAULT_METADATA)
                assertThat(updateProcessingCalls.awaitItem()).isFalse()

                val refreshCall = refresherScenario.refreshCalls.awaitItem()

                assertThat(refreshCall.metadata).isEqualTo(DEFAULT_METADATA)

                assertThat(awaitItem()).containsExactly(paymentMethod)
            }
        }
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = DEFAULT_METADATA,
        collectResult: TapToAddCollectionHandler.CollectionState =
            TapToAddCollectionHandler.CollectionState.Collected(
                PaymentMethodFactory.card(last4 = "4242")
            ),
        refreshedPaymentMethods: List<PaymentMethod> = emptyList(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val updateProcessingCalls = Turbine<Boolean>()
        val updateErrorCalls = Turbine<ResolvableString?>()

        val customerStateHolder = CustomerStateHolder(
            selection = MutableStateFlow(null),
            customerMetadataPermissions = MutableStateFlow(null),
            savedStateHandle = SavedStateHandle(),
        ).apply {
            setCustomerState(
                CustomerState(
                    paymentMethods = emptyList(),
                    defaultPaymentMethodId = null,
                )
            )
        }

        FakeTapToAddCollectionHandler.test(collectResult) {
            val handlerScenario = this

            FakePaymentMethodRefresher.test(Result.success(refreshedPaymentMethods)) {
                block(
                    Scenario(
                        helper = createTapToAddHelper(
                            collectionHandler = handler,
                            paymentMethodRefresher = refresher,
                            metadata = metadata,
                            updateProcessing = {
                                updateProcessingCalls.add(it)
                            },
                            customerStateHolder = customerStateHolder,
                            updateError = {
                                updateErrorCalls.add(it)
                            },
                        ),
                        handlerScenario = handlerScenario,
                        refresherScenario = this,
                        updateProcessingCalls = updateProcessingCalls,
                        customerStateHolder = customerStateHolder,
                        updateErrorCalls = updateErrorCalls,
                    )
                )
            }
        }

        updateProcessingCalls.ensureAllEventsConsumed()
        updateErrorCalls.ensureAllEventsConsumed()
    }

    private suspend fun createTapToAddHelper(
        collectionHandler: TapToAddCollectionHandler,
        paymentMethodRefresher: PaymentMethodRefresher,
        customerStateHolder: CustomerStateHolder,
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
            customerStateHolder = customerStateHolder,
            paymentMethodRefresher = paymentMethodRefresher,
        )
    }

    private class Scenario(
        val helper: TapToAddHelper,
        val customerStateHolder: CustomerStateHolder,
        val handlerScenario: FakeTapToAddCollectionHandler.Scenario,
        val refresherScenario: FakePaymentMethodRefresher.Scenario,
        val updateProcessingCalls: ReceiveTurbine<Boolean>,
        val updateErrorCalls: ReceiveTurbine<ResolvableString?>
    )

    private companion object {
        val DEFAULT_METADATA = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)
    }
}
