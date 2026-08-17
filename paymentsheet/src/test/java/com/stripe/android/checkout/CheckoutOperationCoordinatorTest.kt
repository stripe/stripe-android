@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlin.test.assertFailsWith

@Suppress("LargeClass")
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

            val mutation = testScope.backgroundScope.async {
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

            val mutation = testScope.backgroundScope.async {
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

            val first = testScope.backgroundScope.async {
                coordinator.runMutation {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                    Result.success(Unit)
                }
            }
            firstStarted.await()
            assertThat(awaitItem()).isTrue()

            val second = testScope.backgroundScope.async {
                coordinator.runMutation {
                    secondStarted.complete(Unit)
                    releaseSecond.await()
                    Result.success(Unit)
                }
            }
            testScope.testScheduler.runCurrent()

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

            val first = testScope.backgroundScope.async {
                coordinator.runMutation {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                    Result.success(Unit)
                }
            }
            firstStarted.await()
            assertThat(awaitItem()).isTrue()

            val second = testScope.backgroundScope.async {
                coordinator.runMutation {
                    secondStarted.complete(Unit)
                    Result.success(Unit)
                }
            }
            testScope.testScheduler.runCurrent()

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

            val mutation = testScope.backgroundScope.async {
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
    fun `confirmation is ignored when a payment flow is presented`() = runScenario(
        sheetIsOpen = true,
    ) {
        val arguments = coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        assertThat(arguments).isNull()
        resultTurbine.expectNoEvents()
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `confirmation is ignored while a mutation is in flight`() = runScenario {
        val mutationStarted = CompletableDeferred<Unit>()
        val finishMutation = CompletableDeferred<Unit>()
        val mutation = testScope.async {
            coordinator.runMutation {
                mutationStarted.complete(Unit)
                finishMutation.await()
                Result.success(Unit)
            }
        }
        mutationStarted.await()

        val arguments = coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        assertThat(arguments).isNull()
        resultTurbine.expectNoEvents()

        finishMutation.complete(Unit)
        mutation.await()
    }

    @Test
    fun `second confirmation is ignored while confirmation is in flight`() = runScenario {
        assertThat(coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()

        val arguments = coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        assertThat(arguments).isNull()
        resultTurbine.expectNoEvents()

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
    }

    @Test
    fun `second confirmation can start after first confirmation completes`() = runScenario {
        assertThat(coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()
        sessionRefresher.enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.InformCancellation
            )
        )
        assertThat(sessionRefresher.calls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
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
            val mutation = testScope.async {
                coordinator.runMutation {
                    mutationStarted.complete(Unit)
                    Result.success(Unit)
                }
            }
            testScope.testScheduler.runCurrent()

            assertThat(mutationStarted.isCompleted).isFalse()
            expectNoEvents()

            sessionRefresher.enqueueRefreshAction {}
            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Canceled(
                    ConfirmationHandler.Result.Canceled.Action.InformCancellation
                )
            )
            sessionRefresher.calls.awaitItem()
            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Canceled>()
            mutation.await()

            assertThat(mutationStarted.isCompleted).isTrue()
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `successful confirmation is delivered as completed`() = runScenario {
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `canceled confirmation with modify payment details does not invoke callback and releases gate`() =
        runScenario {
            coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

            sessionRefresher.enqueueRefreshAction {}
            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Canceled(
                    ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails
                )
            )
            assertThat(sessionRefresher.calls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
            testScope.testScheduler.runCurrent()

            resultTurbine.expectNoEvents()
            assertThat(coordinator.isUpdating.value).isFalse()
            assertThat(coordinator.runSynchronousMutation { Result.success(Unit) }.isSuccess).isTrue()
        }

    @Test
    fun `canceled confirmation with no action does not invoke callback and releases gate`() = runScenario {
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        sessionRefresher.enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.None
            )
        )
        assertThat(sessionRefresher.calls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
        testScope.testScheduler.runCurrent()

        resultTurbine.expectNoEvents()
        assertThat(coordinator.isUpdating.value).isFalse()
        assertThat(coordinator.runSynchronousMutation { Result.success(Unit) }.isSuccess).isTrue()
    }

    @Test
    fun `failed confirmation preserves its cause`() = runScenario {
        val expected = IllegalStateException("Confirmation failed")
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        sessionRefresher.enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = expected,
                message = "Confirmation failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        assertThat(sessionRefresher.calls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(expected)
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `confirmation start failure releases the operation gate`() = runScenario {
        val expected = IllegalStateException("Start failed")
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        sessionRefresher.enqueueRefreshAction {}
        coordinator.failConfirmation(expected)

        sessionRefresher.calls.awaitItem()
        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(expected)
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `failed confirmation refreshes the checkout session before delivering the result`() {
        val releaseRefresh = CompletableDeferred<Unit>()
        val expected = IllegalStateException("Confirmation failed")

        runScenario {
            coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

            sessionRefresher.enqueueRefreshAction { releaseRefresh.await() }
            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Failed(
                    cause = expected,
                    message = "Confirmation failed".resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            )

            sessionRefresher.calls.awaitItem()
            resultTurbine.expectNoEvents()
            assertThat(coordinator.isUpdating.value).isTrue()

            releaseRefresh.complete(Unit)

            val result = resultTurbine.awaitItem()
            assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(expected)
            assertThat(coordinator.isUpdating.value).isFalse()
        }
    }

    @Test
    fun `successful response is committed under operation gate before delivering result`() {
        val releaseCommit = CompletableDeferred<Unit>()
        runScenario {
            coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }
            assertThat(coordinator.isUpdating.value).isTrue()

            sessionRefresher.enqueueRefreshAction { releaseCommit.await() }
            confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))
            assertThat(sessionRefresher.calls.awaitItem())
                .isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
            resultTurbine.expectNoEvents()

            val mutationStarted = CompletableDeferred<Unit>()
            val mutation = testScope.backgroundScope.async {
                coordinator.runMutation {
                    mutationStarted.complete(Unit)
                    Result.success(Unit)
                }
            }
            testScope.testScheduler.runCurrent()

            assertThat(mutationStarted.isCompleted).isFalse()
            assertThat(coordinator.isUpdating.value).isTrue()

            releaseCommit.complete(Unit)
            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
            assertThat(mutation.await().isSuccess).isTrue()
            assertThat(coordinator.isUpdating.value).isFalse()
        }
    }

    @Test
    fun `commit failure is logged and still delivers result and releases gate`() {
        val expected = IllegalStateException("Commit failed")
        val logger = FakeLogger()
        runScenario(logger = logger) {
            coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

            sessionRefresher.enqueueRefreshAction { throw expected }
            confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))

            sessionRefresher.calls.awaitItem()
            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
            assertThat(logger.errorLogs).containsExactly(
                "Failed to refresh the checkout session after confirmation." to expected
            )
            assertThat(coordinator.isUpdating.value).isFalse()
            assertThat(coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()
        }
    }

    @Test
    fun `commit cancellation suppresses result and releases gate`() = runScenario {
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        sessionRefresher.enqueueRefreshAction { throw CancellationException("Commit canceled") }
        confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))
        sessionRefresher.calls.awaitItem()
        observerJob.join()

        resultTurbine.expectNoEvents()
        assertThat(observerJob.isCancelled).isTrue()
        assertThat(coordinator.isUpdating.value).isFalse()
        assertThat(coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }).isNotNull()
    }

    @Test
    fun `restored confirmation ignores competing completion while committing`() {
        val releaseCommit = CompletableDeferred<Unit>()
        runScenario(
            initialConfirmationState = ConfirmationHandler.State.Confirming(
                CONFIRMATION_PARAMETERS.confirmationOption
            ),
            hasReloadedFromProcessDeath = true,
        ) {
            sessionRefresher.enqueueRefreshAction { releaseCommit.await() }
            confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))

            assertThat(sessionRefresher.calls.awaitItem())
                .isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))

            coordinator.failConfirmation(IllegalStateException("Competing failure"))
            sessionRefresher.calls.expectNoEvents()
            resultTurbine.expectNoEvents()

            releaseCommit.complete(Unit)

            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
            resultTurbine.expectNoEvents()
            sessionRefresher.calls.expectNoEvents()
        }
    }

    @Test
    fun `successful confirmation without response does not refresh the checkout session`() = runScenario {
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
        )

        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutController.Result.Completed>()
        sessionRefresher.calls.expectNoEvents()
    }

    @Test
    fun `refresh failure still delivers the confirmation result`() = runScenario {
        val expected = IllegalStateException("Confirmation failed")
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        sessionRefresher.enqueueRefreshAction { error("Refresh failed") }
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = expected,
                message = "Confirmation failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        sessionRefresher.calls.awaitItem()
        val result = resultTurbine.awaitItem()
        assertThat((result as CheckoutController.Result.Failed).error).isSameInstanceAs(expected)
        assertThat(coordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `refresh cancellation propagates and still releases the operation gate`() = runScenario {
        coordinator.tryBeginConfirmation { CONFIRMATION_PARAMETERS }

        sessionRefresher.enqueueRefreshAction { throw CancellationException("Refresh canceled") }
        assertFailsWith<CancellationException> {
            coordinator.failConfirmation(IllegalStateException("Start failed"))
        }

        sessionRefresher.calls.awaitItem()
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

            sessionRefresher.enqueueRefreshAction {}
            val failureCompletion = testScope.async(Dispatchers.Default) {
                coordinator.failConfirmation(expected)
            }
            assertThat(callbackStarted.await(5, TimeUnit.SECONDS)).isTrue()

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
            )
            testScope.testScheduler.runCurrent()

            assertThat(deliveredResults).hasSize(1)
            releaseCallback.countDown()
            failureCompletion.await()
            sessionRefresher.calls.awaitItem()

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
        val mutation = testScope.async {
            coordinator.runMutation {
                mutationStarted.complete(Unit)
                Result.success(Unit)
            }
        }
        testScope.testScheduler.runCurrent()
        assertThat(mutationStarted.isCompleted).isFalse()

        sessionRefresher.enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.None
            )
        )
        sessionRefresher.calls.awaitItem()
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
        val mutation = testScope.async {
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
        logger: Logger = Logger.noop(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationState = MutableStateFlow(initialConfirmationState)
        val confirmationHandler = FakeConfirmationHandler(
            hasReloadedFromProcessDeath = hasReloadedFromProcessDeath,
            state = confirmationState,
        )
        val sheetStateHolder = SheetStateHolder(SavedStateHandle()).apply {
            this.sheetIsOpen = sheetIsOpen
        }
        val resultTurbine = Turbine<CheckoutController.Result>()
        val sessionRefresher = FakeCheckoutSessionRefresher()
        val coordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = sheetStateHolder,
            sessionRefresher = sessionRefresher,
            logger = logger,
            resultCallback = resultCallback ?: CheckoutController.ResultCallback(resultTurbine::add),
        )
        val observerJob = backgroundScope.launch {
            coordinator.observeConfirmationResults()
        }
        testScheduler.runCurrent()

        Scenario(
            coordinator = coordinator,
            confirmationState = confirmationState,
            resultTurbine = resultTurbine,
            sessionRefresher = sessionRefresher,
            observerJob = observerJob,
            testScope = this,
        ).block()

        confirmationHandler.validate()
        resultTurbine.ensureAllEventsConsumed()
        sessionRefresher.ensureAllEventsConsumed()
    }

    private class Scenario(
        val coordinator: CheckoutOperationCoordinator,
        val confirmationState: MutableStateFlow<ConfirmationHandler.State>,
        val resultTurbine: Turbine<CheckoutController.Result>,
        val sessionRefresher: FakeCheckoutSessionRefresher,
        val observerJob: Job,
        val testScope: TestScope,
    ) {
        val response = CheckoutSessionResponseFactory.create(id = "cs_confirmed")
    }

    private fun succeededWithSession(
        response: CheckoutSessionResponse,
    ) = ConfirmationHandler.Result.Succeeded(
        intent = PaymentIntentFixtures.PI_SUCCEEDED,
        metadata = MutableConfirmationMetadata().apply {
            set(CheckoutSessionResponseKey, response)
        },
    )
}
