@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class CheckoutOperationCoordinatorTest {

    @Test
    fun `runMutation returns the block result`() = runScenario {
        val result = coordinator.runMutation {
            Result.success("result")
        }

        assertThat(result.getOrThrow()).isEqualTo("result")
    }

    @Test
    fun `isUpdating is true while a mutation runs and false after it completes`() = runScenario {
        val releaseMutation = CompletableDeferred<Unit>()

        coordinator.isUpdating.test {
            assertThat(awaitItem()).isFalse()

            val mutation = backgroundScope.async {
                coordinator.runMutation {
                    releaseMutation.await()
                    Result.success(Unit)
                }
            }

            assertThat(awaitItem()).isTrue()

            releaseMutation.complete(Unit)
            assertThat(mutation.await().isSuccess).isTrue()

            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `failed mutation returns the failure and resets isUpdating`() = runScenario {
        val releaseMutation = CompletableDeferred<Unit>()
        val expectedException = IllegalStateException("Mutation failed")

        coordinator.isUpdating.test {
            assertThat(awaitItem()).isFalse()

            val mutation = backgroundScope.async {
                coordinator.runMutation<Unit> {
                    releaseMutation.await()
                    Result.failure(expectedException)
                }
            }

            assertThat(awaitItem()).isTrue()

            releaseMutation.complete(Unit)
            val result = mutation.await()

            assertThat(result.exceptionOrNull()).isSameInstanceAs(expectedException)
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `mutations are serialized without isUpdating flickering between them`() = runScenario {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val releaseSecond = CompletableDeferred<Unit>()

        coordinator.isUpdating.test {
            assertThat(awaitItem()).isFalse()

            val first = backgroundScope.async {
                coordinator.runMutation {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                    Result.success(Unit)
                }
            }
            firstStarted.await()
            assertThat(awaitItem()).isTrue()

            val second = backgroundScope.async {
                coordinator.runMutation {
                    secondStarted.complete(Unit)
                    releaseSecond.await()
                    Result.success(Unit)
                }
            }
            testScheduler.runCurrent()

            assertThat(secondStarted.isCompleted).isFalse()

            releaseFirst.complete(Unit)
            assertThat(first.await().isSuccess).isTrue()
            secondStarted.await()

            assertThat(coordinator.isUpdating.value).isTrue()
            expectNoEvents()

            releaseSecond.complete(Unit)
            assertThat(second.await().isSuccess).isTrue()

            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `cancelling a queued mutation does not end the active loading window`() = runScenario {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()

        coordinator.isUpdating.test {
            assertThat(awaitItem()).isFalse()

            val first = backgroundScope.async {
                coordinator.runMutation {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                    Result.success(Unit)
                }
            }
            firstStarted.await()
            assertThat(awaitItem()).isTrue()

            val second = backgroundScope.async {
                coordinator.runMutation {
                    secondStarted.complete(Unit)
                    Result.success(Unit)
                }
            }
            testScheduler.runCurrent()

            assertThat(secondStarted.isCompleted).isFalse()

            second.cancelAndJoin()

            assertThat(secondStarted.isCompleted).isFalse()
            assertThat(coordinator.isUpdating.value).isTrue()
            expectNoEvents()

            releaseFirst.complete(Unit)
            assertThat(first.await().isSuccess).isTrue()

            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `throwing mutation resets isUpdating and does not block later mutations`() = runScenario {
        val releaseMutation = CompletableDeferred<Unit>()
        val expectedException = IllegalStateException("Mutation threw")

        coordinator.isUpdating.test {
            assertThat(awaitItem()).isFalse()

            val mutation = backgroundScope.async {
                runCatching {
                    coordinator.runMutation<Unit> {
                        releaseMutation.await()
                        throw expectedException
                    }
                }
            }

            assertThat(awaitItem()).isTrue()

            releaseMutation.complete(Unit)
            val result = mutation.await()

            assertThat(result.exceptionOrNull()).isSameInstanceAs(expectedException)
            assertThat(awaitItem()).isFalse()
        }

        assertThat(coordinator.runMutation { Result.success("next") }.getOrThrow())
            .isEqualTo("next")
    }

    @Test
    fun `confirmation does not start when arguments are unavailable`() = runScenario {
        val arguments = coordinator.beginConfirmation { null }

        assertThat(arguments).isNull()
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `successful confirmation is delivered as completed`() = runScenario {
        coordinator.beginConfirmation { CONFIRMATION_PARAMETERS }

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `canceled confirmation is delivered as canceled`() = runScenario {
        coordinator.beginConfirmation { CONFIRMATION_PARAMETERS }

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails
            )
        )

        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `failed confirmation preserves its cause`() = runScenario {
        val expected = IllegalStateException("Confirmation failed")
        coordinator.beginConfirmation { CONFIRMATION_PARAMETERS }

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = expected,
                message = "Confirmation failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(expected)
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `confirmation start failure releases the operation gate`() = runScenario {
        val expected = IllegalStateException("Start failed")
        coordinator.beginConfirmation { CONFIRMATION_PARAMETERS }

        coordinator.failConfirmation(expected)

        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(expected)
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `overlapping completion signals deliver exactly one result`() {
        val callbackStarted = CountDownLatch(1)
        val releaseCallback = CountDownLatch(1)
        val deliveredResults = CopyOnWriteArrayList<CheckoutController.Result>()
        runScenario(
            resultCallback = CheckoutController.ResultCallback { result ->
                deliveredResults += result
                callbackStarted.countDown()
                check(releaseCallback.await(5, TimeUnit.SECONDS)) {
                    "Timed out waiting to release the result callback."
                }
            },
        ) {
            val expected = IllegalStateException("Start failed")
            assertThat(coordinator.beginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()

            val failureCompletion = async(Dispatchers.Default) {
                coordinator.failConfirmation(expected)
            }
            assertThat(callbackStarted.await(5, TimeUnit.SECONDS)).isTrue()

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
            )
            runCurrent()

            assertThat(deliveredResults).hasSize(1)
            releaseCallback.countDown()
            failureCompletion.await()

            assertThat(deliveredResults).hasSize(1)
            val result = deliveredResults.single()
            assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
            assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(expected)
            assertThat(coordinator.isUpdating.value).isFalse()
        }
    }

    private fun runScenario(
        resultCallback: CheckoutController.ResultCallback? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationState = MutableStateFlow<ConfirmationHandler.State>(ConfirmationHandler.State.Idle)
        val confirmationHandler = FakeConfirmationHandler(
            state = confirmationState,
        )
        val resultTurbine = Turbine<CheckoutController.Result>()
        val coordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            resultCallback = resultCallback ?: CheckoutController.ResultCallback(resultTurbine::add),
        )
        backgroundScope.launch {
            coordinator.observeConfirmationResults()
        }
        testScheduler.runCurrent()

        Scenario(
            coordinator = coordinator,
            confirmationState = confirmationState,
            resultTurbine = resultTurbine,
            testScope = this,
        ).block()

        confirmationHandler.validate()
        resultTurbine.ensureAllEventsConsumed()
    }

    private class Scenario(
        val coordinator: CheckoutOperationCoordinator,
        val confirmationState: MutableStateFlow<ConfirmationHandler.State>,
        val resultTurbine: Turbine<CheckoutController.Result>,
        private val testScope: TestScope,
    ) : CoroutineScope by testScope {
        val backgroundScope = testScope.backgroundScope
        val testScheduler = testScope.testScheduler

        fun runCurrent() {
            testScheduler.runCurrent()
        }
    }
}
