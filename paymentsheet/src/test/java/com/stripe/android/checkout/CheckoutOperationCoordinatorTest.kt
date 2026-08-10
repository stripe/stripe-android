@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
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
        val arguments = coordinator.tryBeginConfirmation { null }

        assertThat(arguments).isNull()
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `confirmation returns failure when a payment flow is presented`() = runScenario(
        sheetIsOpen = true,
    ) {
        val arguments = coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        assertThat(arguments).isNull()
        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        val failure = result as CheckoutController.Result.Failed
        assertThat(failure.error).hasMessageThat()
            .isEqualTo("Cannot confirm checkout session while a payment flow is presented.")
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `confirmation returns failure while a mutation is in flight`() = runScenario {
        val mutationStarted = CompletableDeferred<Unit>()
        val finishMutation = CompletableDeferred<Unit>()
        val mutation = async {
            coordinator.runMutation {
                mutationStarted.complete(Unit)
                finishMutation.await()
                Result.success(Unit)
            }
        }
        mutationStarted.await()

        val arguments = coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        assertThat(arguments).isNull()
        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        val failure = result as CheckoutController.Result.Failed
        assertThat(failure.error).hasMessageThat()
            .isEqualTo("Cannot confirm checkout session while another mutation is in progress.")

        finishMutation.complete(Unit)
        mutation.await()
    }

    @Test
    fun `second confirmation returns failure while confirmation is in flight`() = runScenario {
        assertThat(coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()

        val arguments = coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        assertThat(arguments).isNull()
        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        val failure = result as CheckoutController.Result.Failed
        assertThat(failure.error).hasMessageThat()
            .isEqualTo("Cannot confirm checkout session while another mutation is in progress.")
    }

    @Test
    fun `second confirmation can start after first confirmation completes`() = runScenario {
        assertThat(coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.InformCancellation
            )
        )
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
        assertThat(coordinator.isUpdating.value).isFalse()

        assertThat(coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `mutation waits for confirmation to complete without loading state flicker`() = runScenario {
        coordinator.isUpdating.test {
            assertThat(awaitItem()).isFalse()
            assertThat(coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()
            assertThat(awaitItem()).isTrue()

            val mutationStarted = CompletableDeferred<Unit>()
            val mutation = async {
                coordinator.runMutation {
                    mutationStarted.complete(Unit)
                    Result.success(Unit)
                }
            }
            runCurrent()

            assertThat(mutationStarted.isCompleted).isFalse()
            expectNoEvents()

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Canceled(
                    ConfirmationHandler.Result.Canceled.Action.InformCancellation
                )
            )
            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
            mutation.await()

            assertThat(mutationStarted.isCompleted).isTrue()
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `canceled with ModifyPaymentDetails does not invoke callback and releases gate`() = runScenario {
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails
            )
        )
        runCurrent()

        resultTurbine.expectNoEvents()
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `canceled with None does not invoke callback and releases gate`() = runScenario {
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }
        val mutationStarted = CompletableDeferred<Unit>()
        val mutation = async {
            coordinator.runMutation {
                mutationStarted.complete(Unit)
                Result.success(Unit)
            }
        }
        runCurrent()
        assertThat(mutationStarted.isCompleted).isFalse()

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.None
            )
        )
        mutation.await()

        resultTurbine.expectNoEvents()
        assertThat(mutationStarted.isCompleted).isTrue()
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `confirmation start failure releases the operation gate`() = runScenario {
        val expected = IllegalStateException("Start failed")
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

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
            assertThat(coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()

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

    @Test
    fun `restored confirmation blocks mutations until its result arrives`() = runScenario(
        initialConfirmationState = ConfirmationHandler.State.Confirming(
            CONFIRMATION_PARAMETERS.confirmationOption
        ),
        hasReloadedFromProcessDeath = true,
    ) {
        assertThat(coordinator.isUpdating.value).isTrue()

        val mutationStarted = CompletableDeferred<Unit>()
        val mutation = async {
            coordinator.runMutation {
                mutationStarted.complete(Unit)
                Result.success(Unit)
            }
        }
        runCurrent()
        assertThat(mutationStarted.isCompleted).isFalse()

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.None
            )
        )
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
        mutation.await()

        assertThat(mutationStarted.isCompleted).isTrue()
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `success after process death resets isUpdating and delivers completed`() = runScenario(
        initialConfirmationState = ConfirmationHandler.State.Confirming(
            CONFIRMATION_PARAMETERS.confirmationOption
        ),
        hasReloadedFromProcessDeath = true,
    ) {
        coordinator.isUpdating.test {
            assertThat(awaitItem()).isTrue()

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
            )

            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `synchronous mutation fails while confirmation is in flight`() = runScenario {
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        val result = coordinator.runSynchronousMutation {
            Result.success(Unit)
        }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session while confirmation is in progress.")
    }

    @Test
    fun `synchronous mutation fails while asynchronous mutation is in flight`() = runScenario {
        val mutationStarted = CompletableDeferred<Unit>()
        val finishMutation = CompletableDeferred<Unit>()
        val mutation = async {
            coordinator.runMutation {
                mutationStarted.complete(Unit)
                finishMutation.await()
                Result.success(Unit)
            }
        }
        mutationStarted.await()
        var synchronousMutationInvoked = false

        val result = coordinator.runSynchronousMutation {
            synchronousMutationInvoked = true
            Result.success(Unit)
        }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session while another mutation is in progress.")
        assertThat(synchronousMutationInvoked).isFalse()

        finishMutation.complete(Unit)
        mutation.await()
    }

    @Test
    fun `synchronous mutation executes and returns success when confirmation is not in flight`() = runScenario {
        var invocationCount = 0

        val result = coordinator.runSynchronousMutation {
            invocationCount += 1
            Result.success("updated")
        }

        assertThat(result.getOrThrow()).isEqualTo("updated")
        assertThat(invocationCount).isEqualTo(1)
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    private fun runScenario(
        sheetIsOpen: Boolean = false,
        initialConfirmationState: ConfirmationHandler.State = ConfirmationHandler.State.Idle,
        hasReloadedFromProcessDeath: Boolean = false,
        resultCallback: CheckoutController.ResultCallback? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationState = MutableStateFlow(initialConfirmationState)
        val confirmationHandler = FakeConfirmationHandler(
            hasReloadedFromProcessDeath = hasReloadedFromProcessDeath,
            state = confirmationState,
        )
        val savedStateHandle = SavedStateHandle()
        val sheetStateHolder = SheetStateHolder(savedStateHandle).apply {
            this.sheetIsOpen = sheetIsOpen
        }
        val checkoutStateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
        val resultTurbine = Turbine<CheckoutController.Result>()
        val coordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = sheetStateHolder,
            confirmationResultProcessor =
                CheckoutControllerStateFactory.createConfirmationResultProcessor(checkoutStateHolder),
            resultCallback = resultCallback ?: CheckoutController.ResultCallback { result ->
                resultTurbine.add(result)
            },
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
