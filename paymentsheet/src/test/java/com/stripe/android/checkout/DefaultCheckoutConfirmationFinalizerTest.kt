package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.isInstanceOf
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse
import com.stripe.android.paymentsheet.repositories.CheckoutClientPollResponse.ConfirmationStatus
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class DefaultCheckoutConfirmationFinalizerTest {

    @Test
    fun `complete session finishes without polling`() = runScenario {
        val response = confirmResponse.copy(status = CheckoutSessionResponse.Status.COMPLETE)

        val result = finalizer.finalize(response)

        assertThat(result).isEqualTo(CheckoutConfirmationFinalizer.Result.Completed(response))
    }

    @Test
    fun `expired session fails without polling`() = runScenario {
        val response = confirmResponse.copy(status = CheckoutSessionResponse.Status.EXPIRED)

        val result = finalizer.finalize(response)

        assertThat(result).isInstanceOf<CheckoutConfirmationFinalizer.Result.Failed>()
        result as CheckoutConfirmationFinalizer.Result.Failed
        assertThat(result.response).isEqualTo(response)
        assertThat(result.error).hasMessageThat()
            .isEqualTo("Checkout Session unexpectedly expired after confirmation.")
    }

    @Test
    fun `successful poll applies the polled session state`() = runScenario {
        api.pollResults.add {
            Result.success(
                pollResponse(
                    confirmationStatus = ConfirmationStatus.SUCCEEDED,
                    status = CheckoutSessionResponse.Status.COMPLETE,
                    paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
                )
            )
        }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Poll(SESSION_ID, CLIENT_SECRET))
        assertThat(result).isEqualTo(
            CheckoutConfirmationFinalizer.Result.Completed(
                confirmResponse.copy(
                    status = CheckoutSessionResponse.Status.COMPLETE,
                    paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
                )
            )
        )
    }

    @Test
    fun `timeout without any poll response keeps the confirm response`() = runScenario {
        api.pollResults.add { awaitCancellation() }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Poll(SESSION_ID, CLIENT_SECRET))
        assertThat(result).isEqualTo(CheckoutConfirmationFinalizer.Result.Completed(confirmResponse))
        assertThat(elapsed()).isEqualTo(30.seconds)
    }

    @Test
    fun `timeout applies the last poll response`() = runScenario {
        val pending = pollResponse(
            confirmationStatus = ConfirmationStatus.PENDING,
            status = CheckoutSessionResponse.Status.COMPLETE,
            paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
        )
        api.pollResults.add { Result.success(pending) }
        api.pollResults.add { awaitCancellation() }

        val result = finalizer.finalize(confirmResponse)

        repeat(2) {
            assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Poll(SESSION_ID, CLIENT_SECRET))
        }
        assertThat(result).isEqualTo(
            CheckoutConfirmationFinalizer.Result.Completed(
                confirmResponse.copy(
                    status = CheckoutSessionResponse.Status.COMPLETE,
                    paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
                )
            )
        )
    }

    @Test
    fun `failed poll reports the retrieved payment error`() = runScenario {
        val stripeError = StripeError(type = "card_error", code = "card_declined", message = "Your card was declined.")
        val retrieved = confirmResponse.copy(
            paymentResponse = CheckoutSessionResponse.PaymentResponse(
                status = CheckoutSessionResponse.PaymentResponse.Status.REQUIRES_PAYMENT_METHOD,
                paymentIntent = null,
                setupIntent = null,
                error = stripeError,
            ),
        )
        api.pollResults.add { Result.success(pollResponse(ConfirmationStatus.FAILED)) }
        api.retrieveResult = { Result.success(retrieved) }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Poll(SESSION_ID, CLIENT_SECRET))
        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Retrieve(SESSION_ID, CLIENT_SECRET))
        assertThat(result).isInstanceOf<CheckoutConfirmationFinalizer.Result.Failed>()
        result as CheckoutConfirmationFinalizer.Result.Failed
        assertThat(result.response).isEqualTo(retrieved)
        assertThat(result.error).isInstanceOf<InvalidRequestException>()
        assertThat((result.error as InvalidRequestException).stripeError).isEqualTo(stripeError)
        assertThat(result.error).hasMessageThat().isEqualTo("Your card was declined.")
    }

    @Test
    fun `failed poll without a retrieved payment error reports a generic error`() = runScenario {
        api.pollResults.add { Result.success(pollResponse(ConfirmationStatus.FAILED)) }
        api.retrieveResult = { Result.success(confirmResponse) }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Poll(SESSION_ID, CLIENT_SECRET))
        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Retrieve(SESSION_ID, CLIENT_SECRET))
        result as CheckoutConfirmationFinalizer.Result.Failed
        assertThat(result.response).isEqualTo(confirmResponse)
        assertGenericError(result.error, analyticsValue = "checkoutSessionPaymentFailed")
    }

    @Test
    fun `failed poll with failed retrieval keeps the polled session state`() = runScenario {
        api.pollResults.add {
            Result.success(
                pollResponse(ConfirmationStatus.FAILED, status = CheckoutSessionResponse.Status.COMPLETE)
            )
        }
        api.retrieveResult = { Result.failure(APIConnectionException()) }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Poll(SESSION_ID, CLIENT_SECRET))
        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Retrieve(SESSION_ID, CLIENT_SECRET))
        result as CheckoutConfirmationFinalizer.Result.Failed
        assertThat(result.response).isEqualTo(confirmResponse.copy(status = CheckoutSessionResponse.Status.COMPLETE))
        assertGenericError(result.error, analyticsValue = "pollFailedAndSessionRetrievalFailed")
    }

    @Test
    fun `failed poll gives up on a slow retrieval after five seconds`() = runScenario {
        api.pollResults.add { Result.success(pollResponse(ConfirmationStatus.FAILED)) }
        api.retrieveResult = { awaitCancellation() }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Poll(SESSION_ID, CLIENT_SECRET))
        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Retrieve(SESSION_ID, CLIENT_SECRET))
        assertThat(elapsed()).isEqualTo(5.seconds)
        result as CheckoutConfirmationFinalizer.Result.Failed
        assertGenericError(result.error, analyticsValue = "pollFailedAndSessionRetrievalFailed")
    }

    @Test
    fun `not started fails and reports an unexpected error`() = runScenario {
        api.pollResults.add { Result.success(pollResponse(ConfirmationStatus.NOT_STARTED)) }

        val result = finalizer.finalize(confirmResponse)

        assertThat(api.calls.awaitItem()).isEqualTo(FakeCheckoutSessionApi.Call.Poll(SESSION_ID, CLIENT_SECRET))
        assertThat(errorReporter.awaitCall().errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.CHECKOUT_SESSION_CONFIRMATION_NOT_STARTED)
        result as CheckoutConfirmationFinalizer.Result.Failed
        assertThat(result.response).isEqualTo(confirmResponse)
        assertThat(result.error).hasMessageThat().isEqualTo(
            "Checkout Session confirmation unexpectedly reported not_started after an accepted /confirm."
        )
    }

    private fun assertGenericError(error: Throwable, analyticsValue: String) {
        assertThat(error).isInstanceOf<LocalStripeException>()
        error as LocalStripeException
        assertThat(error.displayMessage).isEqualTo(GENERIC_ERROR_MESSAGE)
        assertThat(error.analyticsValue()).isEqualTo(analyticsValue)
    }

    private fun pollResponse(
        confirmationStatus: ConfirmationStatus,
        status: CheckoutSessionResponse.Status = CheckoutSessionResponse.Status.OPEN,
        paymentStatus: CheckoutSessionResponse.PaymentStatus = CheckoutSessionResponse.PaymentStatus.UNPAID,
    ) = CheckoutClientPollResponse(
        id = SESSION_ID,
        confirmationStatus = confirmationStatus,
        status = status,
        paymentStatus = paymentStatus,
    )

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val api = FakeCheckoutSessionApi()
        val errorReporter = FakeErrorReporter()
        val start = testScheduler.timeSource.markNow()
        val finalizer = DefaultCheckoutConfirmationFinalizer(
            poller = CheckoutSessionPoller(testScheduler.timeSource),
            poll = api::poll,
            retrieve = api::retrieve,
            errorReporter = errorReporter,
            genericErrorMessage = GENERIC_ERROR_MESSAGE,
        )

        Scenario(
            finalizer = finalizer,
            api = api,
            errorReporter = errorReporter,
            elapsed = { start.elapsedNow() },
        ).block()

        api.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    private class Scenario(
        val finalizer: DefaultCheckoutConfirmationFinalizer,
        val api: FakeCheckoutSessionApi,
        val errorReporter: FakeErrorReporter,
        val elapsed: () -> Duration,
    ) {
        val confirmResponse = CheckoutSessionResponseFactory.create(
            id = SESSION_ID,
            clientSecret = CLIENT_SECRET,
        )
    }

    private class FakeCheckoutSessionApi {
        val calls = Turbine<Call>()
        val pollResults = ArrayDeque<suspend () -> Result<CheckoutClientPollResponse>>()
        var retrieveResult: suspend () -> Result<CheckoutSessionResponse> = {
            error("Unexpected retrieve")
        }

        suspend fun poll(sessionId: String, clientSecret: String): Result<CheckoutClientPollResponse> {
            calls.add(Call.Poll(sessionId, clientSecret))
            return pollResults.removeFirst().invoke()
        }

        suspend fun retrieve(sessionId: String, clientSecret: String): Result<CheckoutSessionResponse> {
            calls.add(Call.Retrieve(sessionId, clientSecret))
            return retrieveResult()
        }

        fun ensureAllEventsConsumed() {
            calls.ensureAllEventsConsumed()
            assertThat(pollResults).isEmpty()
        }

        sealed interface Call {
            data class Poll(val sessionId: String, val clientSecret: String) : Call
            data class Retrieve(val sessionId: String, val clientSecret: String) : Call
        }
    }

    private companion object {
        const val SESSION_ID = "cs_test_finalize"
        const val CLIENT_SECRET = "cs_test_finalize_secret_test"
        const val GENERIC_ERROR_MESSAGE = "Something went wrong"
    }
}
