@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.assertFailsWith

@Suppress("LargeClass")
@RunWith(RobolectricTestRunner::class)
internal class CheckoutOperationCoordinatorTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `runMutation returns the block result`() = runScenario {
        val result = coordinator.runMutation {
            Result.success("result")
        }

        assertThat(result.getOrThrow()).isEqualTo("result")
    }

    @Test
    fun `status is Updating while a mutation runs and Idle after it completes`() = runScenario {
        val releaseMutation = CompletableDeferred<Unit>()

        coordinator.status.test {
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)

            val mutation = backgroundScope.async {
                coordinator.runMutation {
                    releaseMutation.await()
                    Result.success(Unit)
                }
            }

            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

            releaseMutation.complete(Unit)
            assertThat(mutation.await().isSuccess).isTrue()

            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }
    }

    @Test
    fun `failed mutation returns the failure and resets status`() = runScenario {
        val releaseMutation = CompletableDeferred<Unit>()
        val expectedException = IllegalStateException("Mutation failed")

        coordinator.status.test {
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)

            val mutation = backgroundScope.async {
                coordinator.runMutation<Unit> {
                    releaseMutation.await()
                    Result.failure(expectedException)
                }
            }

            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

            releaseMutation.complete(Unit)
            val result = mutation.await()

            assertThat(result.exceptionOrNull()).isSameInstanceAs(expectedException)
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }
    }

    @Test
    fun `mutations are serialized without status flickering between them`() = runScenario {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val releaseSecond = CompletableDeferred<Unit>()

        coordinator.status.test {
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)

            val first = backgroundScope.async {
                coordinator.runMutation {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                    Result.success(Unit)
                }
            }
            firstStarted.await()
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

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

            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)
            expectNoEvents()

            releaseSecond.complete(Unit)
            assertThat(second.await().isSuccess).isTrue()

            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }
    }

    @Test
    fun `cancelling a queued mutation does not end the active loading window`() = runScenario {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()

        coordinator.status.test {
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)

            val first = backgroundScope.async {
                coordinator.runMutation {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                    Result.success(Unit)
                }
            }
            firstStarted.await()
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

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
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)
            expectNoEvents()

            releaseFirst.complete(Unit)
            assertThat(first.await().isSuccess).isTrue()

            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }
    }

    @Test
    fun `throwing mutation resets status and does not block later mutations`() = runScenario {
        val releaseMutation = CompletableDeferred<Unit>()
        val expectedException = IllegalStateException("Mutation threw")

        coordinator.status.test {
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)

            val mutation = backgroundScope.async {
                runCatching {
                    coordinator.runMutation<Unit> {
                        releaseMutation.await()
                        throw expectedException
                    }
                }
            }

            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

            releaseMutation.complete(Unit)
            val result = mutation.await()

            assertThat(result.exceptionOrNull()).isSameInstanceAs(expectedException)
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }

        assertThat(coordinator.runMutation { Result.success("next") }.getOrThrow())
            .isEqualTo("next")
    }

    @Test
    fun `argument validation failure reports its cause and allows retry`() = runScenario {
        val expected = IllegalStateException("Invalid confirmation arguments")
        val arguments = coordinator.tryBeginConfirmation { Result.failure(expected) }

        assertThat(arguments).isNull()
        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutElementsController.Result.Failed>()
        val failedResult = result as CheckoutElementsController.Result.Failed
        assertThat(failedResult.error).isSameInstanceAs(expected)
        assertThat(failedResult.displayMessage)
            .isEqualTo(applicationContext.getString(R.string.stripe_something_went_wrong))
        resultTurbine.expectNoEvents()
        refreshCalls.expectNoEvents()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
    }

    @Test
    fun `rejection callback runs outside admission lock`() {
        lateinit var operationCoordinator: CheckoutOperationCoordinator
        val mutationResults = Turbine<Result<Unit>>()
        val callbackResults = Turbine<CheckoutElementsController.Result>()
        runScenario(
            resultCallback = CheckoutElementsController.ResultCallback { result ->
                val mutationFinished = CountDownLatch(1)
                thread(isDaemon = true) {
                    try {
                        runBlocking {
                            mutationResults.add(operationCoordinator.runMutation { Result.success(Unit) })
                        }
                    } finally {
                        mutationFinished.countDown()
                    }
                }
                check(mutationFinished.await(5, TimeUnit.SECONDS)) {
                    "Rejection callback must allow another thread to acquire the admission lock."
                }
                callbackResults.add(result)
            },
        ) {
            operationCoordinator = coordinator
            val expected = IllegalStateException("Invalid confirmation arguments")
            assertThat(coordinator.tryBeginConfirmation { Result.failure(expected) }).isNull()

            val result = callbackResults.awaitItem()
            assertThat(result).isInstanceOf<CheckoutElementsController.Result.Failed>()
            assertThat((result as CheckoutElementsController.Result.Failed).error).isSameInstanceAs(expected)
            assertThat(mutationResults.awaitItem().isSuccess).isTrue()
            runCurrent()
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }
        mutationResults.ensureAllEventsConsumed()
        callbackResults.ensureAllEventsConsumed()
    }

    @Test
    fun `confirmation fails when a payment flow is presented and can retry after dismissal`() = runScenario(
        sheetIsOpen = true,
    ) {
        val arguments = coordinator.tryBeginConfirmation { error("Arguments must not be built while busy") }

        assertThat(arguments).isNull()
        assertRejection("Cannot confirm checkout while a payment flow is presented.")
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)

        sheetStateHolder.sheetIsOpen = false
        assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
    }

    @Test
    fun `confirmation fails while a mutation is in flight and can retry after it finishes`() = runScenario {
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

        val arguments = coordinator.tryBeginConfirmation { error("Arguments must not be built while busy") }

        assertThat(arguments).isNull()
        assertRejection("Cannot confirm checkout while checkout mutations are in progress.")
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

        finishMutation.complete(Unit)
        mutation.await()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
    }

    @Test
    fun `confirmation fails while a mutation is queued behind confirmation`() = runScenario {
        assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
        val mutationStarted = CompletableDeferred<Unit>()
        val mutation = async {
            coordinator.runMutation {
                mutationStarted.complete(Unit)
                Result.success(Unit)
            }
        }
        runCurrent()
        assertThat(mutationStarted.isCompleted).isFalse()

        assertThat(coordinator.tryBeginConfirmation { error("Arguments must not be built while busy") }).isNull()
        assertRejection("Cannot confirm checkout while checkout mutations are in progress.")
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)
        assertThat(mutationStarted.isCompleted).isFalse()

        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))
        assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)
        assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Completed>()
        assertThat(mutation.await().isSuccess).isTrue()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
    }

    @Test
    fun `each repeat confirmation fails while the original completes normally`() = runScenario {
        assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()

        repeat(2) {
            val arguments = coordinator.tryBeginConfirmation { error("Arguments must not be built while busy") }

            assertThat(arguments).isNull()
            assertRejection("Cannot confirm checkout while another confirmation is in progress.")
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)
        }

        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))
        assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)
        assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Completed>()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
    }

    @Test
    fun `restored confirmation rejects confirm before its handler resumes`() = runScenario(
        hasReloadedFromProcessDeath = true,
    ) {
        assertThat(coordinator.tryBeginConfirmation { error("Arguments must not be built while busy") }).isNull()
        assertRejection("Cannot confirm checkout while another confirmation is in progress.")
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(ConfirmationHandler.Result.Canceled.Action.None)
        )
        refreshCalls.awaitItem()
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Canceled>()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
    }

    @Test
    fun `existing handler confirmation rejects confirm and still completes`() = runScenario(
        initialConfirmationState = ConfirmationHandler.State.Confirming(CONFIRMATION_PARAMETERS.confirmationOption),
    ) {
        assertThat(coordinator.tryBeginConfirmation { error("Arguments must not be built while busy") }).isNull()
        assertRejection("Cannot confirm checkout while another confirmation is in progress.")
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))
        assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)
        assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Completed>()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
    }

    @Test
    fun `second confirmation can start after first confirmation completes`() = runScenario {
        assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.InformCancellation
            )
        )
        assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Canceled>()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)

        assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))

        assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)
        assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Completed>()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
    }

    @Test
    fun `mutation waits for confirmation to complete without loading state flicker`() = runScenario {
        coordinator.status.test {
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
            assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

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

            enqueueRefreshAction {}
            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Canceled(
                    ConfirmationHandler.Result.Canceled.Action.InformCancellation
                )
            )
            refreshCalls.awaitItem()
            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Canceled>()
            mutation.await()

            assertThat(mutationStarted.isCompleted).isTrue()
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }
    }

    @Test
    fun `completed session returns unpaid even when the intent succeeded`() = runScenario {
        assertCompletedPaymentStatus(
            finalized = response.copy(status = CheckoutSessionResponse.Status.COMPLETE),
            expected = CheckoutElementsController.Session.Status.PaymentStatus.Unpaid,
        )
    }

    @Test
    fun `completed session returns no payment required`() = runScenario {
        assertCompletedPaymentStatus(
            finalized = response.copy(
                status = CheckoutSessionResponse.Status.COMPLETE,
                paymentStatus = CheckoutSessionResponse.PaymentStatus.NO_PAYMENT_REQUIRED,
            ),
            expected = CheckoutElementsController.Session.Status.PaymentStatus.NoPaymentRequired,
        )
    }

    @Test
    fun `optimistic success with an open session returns its last reported payment status`() = runScenario {
        assertCompletedPaymentStatus(
            finalized = response,
            expected = CheckoutElementsController.Session.Status.PaymentStatus.Unpaid,
        )
    }

    @Test
    fun `canceled confirmation with modify payment details does not invoke callback and releases gate`() =
        runScenario {
            coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

            enqueueRefreshAction {}
            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Canceled(
                    ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails
                )
            )
            assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
            runCurrent()

            resultTurbine.expectNoEvents()
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
            assertThat(coordinator.runMutation { Result.success(Unit) }.isSuccess).isTrue()
        }

    @Test
    fun `canceled confirmation with no action does not invoke callback and releases gate`() = runScenario {
        coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.None
            )
        )
        assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
        runCurrent()

        resultTurbine.expectNoEvents()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        assertThat(coordinator.runMutation { Result.success(Unit) }.isSuccess).isTrue()
    }

    @Test
    fun `failed confirmation preserves its cause and server display message`() = runScenario {
        val expected = APIException(
            stripeError = StripeError(type = "api_error", message = "Server confirmation message"),
        )
        coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = expected,
                message = "Confirmation fallback message".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
            )
        )

        assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutElementsController.Result.Failed>()
        val failedResult = result as CheckoutElementsController.Result.Failed
        assertThat(failedResult.error).isSameInstanceAs(expected)
        assertThat(failedResult.displayMessage).isEqualTo("Server confirmation message")
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
    }

    @Test
    fun `confirmation start failure releases the operation gate`() = runScenario {
        val expected = IllegalStateException("Start failed")
        coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

        enqueueRefreshAction {}
        coordinator.failConfirmation(expected)

        refreshCalls.awaitItem()
        val result = resultTurbine.awaitItem()
        assertThat(result).isInstanceOf<CheckoutElementsController.Result.Failed>()
        val failedResult = result as CheckoutElementsController.Result.Failed
        assertThat(failedResult.error).isSameInstanceAs(expected)
        assertThat(failedResult.displayMessage)
            .isEqualTo(applicationContext.getString(R.string.stripe_something_went_wrong))
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
    }

    @Test
    fun `failed confirmation refreshes the checkout session before delivering the result`() {
        val releaseRefresh = CompletableDeferred<Unit>()
        val expected = IllegalStateException("Confirmation failed")

        runScenario {
            coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

            enqueueRefreshAction { releaseRefresh.await() }
            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Failed(
                    cause = expected,
                    message = "Confirmation failed".resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
                )
            )

            refreshCalls.awaitItem()
            resultTurbine.expectNoEvents()
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

            releaseRefresh.complete(Unit)

            val result = resultTurbine.awaitItem()
            val failedResult = result as CheckoutElementsController.Result.Failed
            assertThat(failedResult.error).isSameInstanceAs(expected)
            assertThat(failedResult.displayMessage)
                .isEqualTo(applicationContext.getString(R.string.stripe_something_went_wrong))
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }
    }

    @Test
    fun `successful response is committed under operation gate before delivering result`() {
        val releaseCommit = CompletableDeferred<Unit>()
        runScenario {
            coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

            enqueueRefreshAction { releaseCommit.await() }
            confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))
            assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)
            assertThat(refreshCalls.awaitItem())
                .isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
            resultTurbine.expectNoEvents()

            val mutationStarted = CompletableDeferred<Unit>()
            val mutation = backgroundScope.async {
                coordinator.runMutation {
                    mutationStarted.complete(Unit)
                    Result.success(Unit)
                }
            }
            runCurrent()

            assertThat(mutationStarted.isCompleted).isFalse()
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

            releaseCommit.complete(Unit)
            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Completed>()
            assertThat(mutation.await().isSuccess).isTrue()
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }
    }

    @Test
    fun `commit failure is logged and still delivers result and releases gate`() {
        val expected = IllegalStateException("Commit failed")
        val logger = FakeLogger()
        runScenario(logger = logger) {
            val finalized = response.copy(paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID)
            confirmationFinalizer.result = { Result.success(finalized) }
            coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

            enqueueRefreshAction { throw expected }
            confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))
            assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)

            assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(finalized))
            val result = resultTurbine.awaitItem() as CheckoutElementsController.Result.Completed
            assertThat(result.paymentStatus).isEqualTo(CheckoutElementsController.Session.Status.PaymentStatus.Paid)
            assertThat(logger.errorLogs).containsExactly(
                "Failed to refresh the checkout session after confirmation." to expected
            )
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
            assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
        }
    }

    @Test
    fun `commit cancellation suppresses result and releases gate`() = runScenario {
        coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

        enqueueRefreshAction { throw CancellationException("Commit canceled") }
        confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))
        assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)
        refreshCalls.awaitItem()
        observerJob.join()

        resultTurbine.expectNoEvents()
        assertThat(observerJob.isCancelled).isTrue()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
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
            enqueueRefreshAction { releaseCommit.await() }
            confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))
            assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)

            assertThat(refreshCalls.awaitItem())
                .isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))

            coordinator.failConfirmation(IllegalStateException("Competing failure"))
            refreshCalls.expectNoEvents()
            resultTurbine.expectNoEvents()

            releaseCommit.complete(Unit)

            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Completed>()
            resultTurbine.expectNoEvents()
            refreshCalls.expectNoEvents()
        }
    }

    @Test
    fun `accepted confirmation commits and reports the finalized response`() {
        val reporter = FakeCheckoutConfirmationResultReporter()
        runScenario(resultReporter = reporter) {
            val finalized = response.copy(
                status = CheckoutSessionResponse.Status.COMPLETE,
                paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
            )
            confirmationFinalizer.result = { Result.success(finalized) }
            coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

            enqueueRefreshAction {}
            val succeeded = succeededWithSession(response)
            confirmationState.value = ConfirmationHandler.State.Complete(succeeded)

            assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)
            assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(finalized))
            assertThat(reporter.calls.awaitItem())
                .isEqualTo(FakeCheckoutConfirmationResultReporter.Call(succeeded, finalized))
            val result = resultTurbine.awaitItem() as CheckoutElementsController.Result.Completed
            assertThat(result.paymentStatus).isEqualTo(CheckoutElementsController.Session.Status.PaymentStatus.Paid)
        }
        reporter.ensureAllEventsConsumed()
    }

    @Test
    fun `next action intent replaces the intent from the confirm response before finalizing`() = runScenario {
        val confirmResponse = response.withIntent(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)
        coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(confirmResponse))

        val finalized = confirmationFinalizer.calls.awaitItem()
        assertThat(finalized.paymentIntent).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED)
        assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(finalized))
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Completed>()
    }

    @Test
    fun `failed finalization refreshes the session and delivers the failure`() {
        val reporter = FakeCheckoutConfirmationResultReporter()
        runScenario(resultReporter = reporter) {
            val error = IllegalStateException("Checkout Session confirmation failed.")
            confirmationFinalizer.result = { Result.failure(error) }
            coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

            enqueueRefreshAction {}
            confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))

            assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)
            assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)

            val reported = reporter.calls.awaitItem()
            assertThat(reported.response).isNull()
            assertThat(reported.result).isInstanceOf<ConfirmationHandler.Result.Failed>()
            reported.result as ConfirmationHandler.Result.Failed
            assertThat(reported.result.cause).isSameInstanceAs(error)
            assertThat(reported.result.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Transaction())

            val result = resultTurbine.awaitItem()
            assertThat(result).isInstanceOf<CheckoutElementsController.Result.Failed>()
            result as CheckoutElementsController.Result.Failed
            assertThat(result.error).isSameInstanceAs(error)
        }
        reporter.ensureAllEventsConsumed()
    }

    @Test
    fun `failed confirmation refreshes and reports without a confirm response`() {
        val reporter = FakeCheckoutConfirmationResultReporter()
        runScenario(resultReporter = reporter) {
            coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

            enqueueRefreshAction {}
            val failed = ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("Next action failed"),
                message = "Next action failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
            )
            confirmationState.value = ConfirmationHandler.State.Complete(failed)

            assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
            assertThat(reporter.calls.awaitItem())
                .isEqualTo(FakeCheckoutConfirmationResultReporter.Call(failed, response = null))
            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Failed>()
        }
        reporter.ensureAllEventsConsumed()
    }

    @Test
    fun `successful confirmation without response maps to failure for callback and reporting`() {
        val reporter = FakeCheckoutConfirmationResultReporter()
        runScenario(resultReporter = reporter) {
            coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

            enqueueRefreshAction {}
            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
            )

            assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
            val result = resultTurbine.awaitItem() as CheckoutElementsController.Result.Failed
            assertThat(result.error).hasMessageThat().isEqualTo(
                "Checkout confirmation succeeded without returning the confirmed session."
            )
            assertThat(result.displayMessage)
                .isEqualTo(applicationContext.getString(R.string.stripe_something_went_wrong))
            val reported = reporter.calls.awaitItem()
            assertThat(reported.response).isNull()
            val reportedFailure = reported.result as ConfirmationHandler.Result.Failed
            assertThat(reportedFailure.cause).isSameInstanceAs(result.error)
            assertThat(reportedFailure.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
            assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
        }
        reporter.ensureAllEventsConsumed()
    }

    @Test
    fun `missing response refresh finishes under operation gate before delivering failure`() {
        val releaseRefresh = CompletableDeferred<Unit>()
        runScenario {
            coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

            enqueueRefreshAction { releaseRefresh.await() }
            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
            )
            assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
            resultTurbine.expectNoEvents()

            val mutationStarted = CompletableDeferred<Unit>()
            val mutation = backgroundScope.async {
                coordinator.runMutation {
                    mutationStarted.complete(Unit)
                    Result.success(Unit)
                }
            }
            runCurrent()

            assertThat(mutationStarted.isCompleted).isFalse()
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

            releaseRefresh.complete(Unit)
            assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Failed>()
            assertThat(mutation.await().isSuccess).isTrue()
        }
    }

    @Test
    fun `refresh failure without a confirmed response is logged and delivers failure`() {
        val expected = IllegalStateException("Refresh failed")
        val logger = FakeLogger()
        runScenario(logger = logger) {
            coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

            enqueueRefreshAction { throw expected }
            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
            )

            assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Fetch)
            val result = resultTurbine.awaitItem() as CheckoutElementsController.Result.Failed
            assertThat(result.error).hasMessageThat().isEqualTo(
                "Checkout confirmation succeeded without returning the confirmed session."
            )
            assertThat(logger.errorLogs).containsExactly(
                "Failed to refresh the checkout session after confirmation." to expected
            )
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
            assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()
        }
    }

    @Test
    fun `refresh failure still delivers the confirmation result`() = runScenario {
        val expected = IllegalStateException("Confirmation failed")
        coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

        enqueueRefreshAction { error("Refresh failed") }
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = expected,
                message = "Confirmation failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Transaction(),
            )
        )

        refreshCalls.awaitItem()
        val result = resultTurbine.awaitItem()
        assertThat((result as CheckoutElementsController.Result.Failed).error).isSameInstanceAs(expected)
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
    }

    @Test
    fun `refresh cancellation propagates and still releases the operation gate`() = runScenario {
        coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }

        enqueueRefreshAction { throw CancellationException("Refresh canceled") }
        assertFailsWith<CancellationException> {
            coordinator.failConfirmation(IllegalStateException("Start failed"))
        }

        refreshCalls.awaitItem()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
    }

    @Test
    fun `overlapping completion signals deliver exactly one result`() {
        val callbackStarted = CountDownLatch(1)
        val releaseCallback = CountDownLatch(1)
        val deliveredResults = CopyOnWriteArrayList<CheckoutElementsController.Result>()
        runScenario(
            resultCallback = CheckoutElementsController.ResultCallback { result ->
                deliveredResults += result
                callbackStarted.countDown()
                check(releaseCallback.await(5, TimeUnit.SECONDS)) {
                    "Timed out waiting to release the result callback."
                }
            },
        ) {
            val expected = IllegalStateException("Start failed")
            assertThat(coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }).isNotNull()

            enqueueRefreshAction {}
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
            refreshCalls.awaitItem()

            assertThat(deliveredResults).hasSize(1)
            val result = deliveredResults.single()
            assertThat(result).isInstanceOf<CheckoutElementsController.Result.Failed>()
            assertThat((result as CheckoutElementsController.Result.Failed).error).isSameInstanceAs(expected)
            assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }
    }

    @Test
    fun `restored confirmation blocks mutations until its result arrives`() = runScenario(
        initialConfirmationState = ConfirmationHandler.State.Confirming(
            CONFIRMATION_PARAMETERS.confirmationOption
        ),
        hasReloadedFromProcessDeath = true,
    ) {
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Updating)

        val mutationStarted = CompletableDeferred<Unit>()
        val mutation = async {
            coordinator.runMutation {
                mutationStarted.complete(Unit)
                Result.success(Unit)
            }
        }
        runCurrent()
        assertThat(mutationStarted.isCompleted).isFalse()

        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.None
            )
        )
        refreshCalls.awaitItem()
        assertThat(resultTurbine.awaitItem()).isInstanceOf<CheckoutElementsController.Result.Canceled>()
        mutation.await()

        assertThat(mutationStarted.isCompleted).isTrue()
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
    }

    @Test
    fun `success after process death resets status and delivers completed`() = runScenario(
        initialConfirmationState = ConfirmationHandler.State.Confirming(
            CONFIRMATION_PARAMETERS.confirmationOption
        ),
        hasReloadedFromProcessDeath = true,
    ) {
        coordinator.status.test {
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Updating)
            assertThat(coordinator.tryBeginConfirmation { error("Arguments must not be built while busy") }).isNull()
            assertRejection("Cannot confirm checkout while another confirmation is in progress.")
            expectNoEvents()

            enqueueRefreshAction {}
            confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))

            assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)
            assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
            val result = resultTurbine.awaitItem() as CheckoutElementsController.Result.Completed
            assertThat(result.paymentStatus).isEqualTo(CheckoutElementsController.Session.Status.PaymentStatus.Unpaid)
            assertThat(awaitItem()).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
        }
    }

    private fun runScenario(
        sheetIsOpen: Boolean = false,
        initialConfirmationState: ConfirmationHandler.State = ConfirmationHandler.State.Idle,
        hasReloadedFromProcessDeath: Boolean = false,
        resultCallback: CheckoutElementsController.ResultCallback? = null,
        logger: Logger = Logger.noop(),
        resultReporter: CheckoutConfirmationResultReporter = CheckoutConfirmationResultReporter { _, _ -> },
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
        val resultTurbine = Turbine<CheckoutElementsController.Result>()
        val sessionRefresher = FakeCheckoutSessionRefresher()
        val confirmationFinalizer = FakeCheckoutConfirmationFinalizer()
        val coordinator = CheckoutOperationCoordinator(
            displayMessageResolver = CheckoutDisplayMessageResolver(applicationContext),
            confirmationHandler = confirmationHandler,
            sheetStateHolder = sheetStateHolder,
            sessionRefresher = sessionRefresher,
            confirmationFinalizer = confirmationFinalizer,
            resultReporter = resultReporter,
            logger = logger,
            resultCallback = resultCallback ?: CheckoutElementsController.ResultCallback(resultTurbine::add),
            viewModelScope = backgroundScope,
        )
        val observerJob = backgroundScope.launch {
            coordinator.observeConfirmationResults()
        }
        testScheduler.runCurrent()

        Scenario(
            coordinator = coordinator,
            sheetStateHolder = sheetStateHolder,
            confirmationState = confirmationState,
            resultTurbine = resultTurbine,
            refreshCalls = sessionRefresher.calls,
            sessionRefresher = sessionRefresher,
            confirmationFinalizer = confirmationFinalizer,
            observerJob = observerJob,
            testScope = this,
        ).block()

        confirmationHandler.validate()
        resultTurbine.ensureAllEventsConsumed()
        sessionRefresher.ensureAllEventsConsumed()
        confirmationFinalizer.ensureAllEventsConsumed()
    }

    private class Scenario(
        val coordinator: CheckoutOperationCoordinator,
        val sheetStateHolder: SheetStateHolder,
        val confirmationState: MutableStateFlow<ConfirmationHandler.State>,
        val resultTurbine: Turbine<CheckoutElementsController.Result>,
        val refreshCalls: Turbine<FakeCheckoutSessionRefresher.Call>,
        private val sessionRefresher: FakeCheckoutSessionRefresher,
        val confirmationFinalizer: FakeCheckoutConfirmationFinalizer,
        val observerJob: Job,
        private val testScope: TestScope,
    ) : CoroutineScope by testScope {
        val response = CheckoutSessionResponseFactory.create(id = "cs_confirmed")
            .withIntent(PaymentIntentFixtures.PI_SUCCEEDED)
        val backgroundScope = testScope.backgroundScope
        val testScheduler = testScope.testScheduler

        suspend fun assertRejection(message: String) {
            val result = resultTurbine.awaitItem()
            assertThat(result).isInstanceOf<CheckoutElementsController.Result.Failed>()
            val error = (result as CheckoutElementsController.Result.Failed).error
            assertThat(error).isInstanceOf<IllegalStateException>()
            assertThat(error).hasMessageThat().isEqualTo(message)
            resultTurbine.expectNoEvents()
            refreshCalls.expectNoEvents()
        }

        fun runCurrent() {
            testScheduler.runCurrent()
        }

        fun enqueueRefreshAction(action: suspend () -> Unit) {
            sessionRefresher.enqueueRefreshAction(action)
        }
    }

    private suspend fun Scenario.assertCompletedPaymentStatus(
        finalized: CheckoutSessionResponse,
        expected: CheckoutElementsController.Session.Status.PaymentStatus,
    ) {
        confirmationFinalizer.result = { Result.success(finalized) }
        coordinator.tryBeginConfirmation { Result.success(CONFIRMATION_PARAMETERS) }
        enqueueRefreshAction {}
        confirmationState.value = ConfirmationHandler.State.Complete(succeededWithSession(response))

        assertThat(confirmationFinalizer.calls.awaitItem()).isEqualTo(response)
        assertThat(refreshCalls.awaitItem()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(finalized))
        val result = resultTurbine.awaitItem() as CheckoutElementsController.Result.Completed
        assertThat(result.paymentStatus).isEqualTo(expected)
        assertThat(coordinator.status.value).isEqualTo(CheckoutOperationCoordinator.Status.Idle)
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
