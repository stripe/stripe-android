package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakePollingAnalyticsEventReporter
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UpiFlowResultProcessorTest {
    @Test
    fun `ambiguous app return polls until Stripe confirms payment`() = runScenario(
        statuses = listOf(
            StripeIntent.Status.RequiresAction,
            StripeIntent.Status.RequiresAction,
            StripeIntent.Status.Succeeded,
        )
    ) {
        val result = processor.processResult(RETURN_RESULT).getOrThrow()

        assertThat(result.outcome).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        assertThat(result.intent.status).isEqualTo(StripeIntent.Status.Succeeded)
        repeat(3) { assertRetrieveCall() }
    }

    @Test
    fun `processing on first retrieve continues polling until success`() = runScenario(
        statuses = listOf(StripeIntent.Status.Processing, StripeIntent.Status.Processing, StripeIntent.Status.Succeeded)
    ) {
        val result = processor.processResult(RETURN_RESULT).getOrThrow()

        assertThat(result.outcome).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        repeat(3) { assertRetrieveCall() }
    }

    @Test
    fun `transition from requires action through processing does not end polling early`() = runScenario(
        statuses = listOf(
            StripeIntent.Status.RequiresAction,
            StripeIntent.Status.Processing,
            StripeIntent.Status.Succeeded,
        )
    ) {
        val result = processor.processResult(RETURN_RESULT).getOrThrow()

        assertThat(result.intent.status).isEqualTo(StripeIntent.Status.Succeeded)
        repeat(3) { assertRetrieveCall() }
    }

    @Test
    fun `canceled app result still verifies payment with Stripe`() = runScenario(
        statuses = listOf(StripeIntent.Status.RequiresAction, StripeIntent.Status.Succeeded)
    ) {
        val result = processor.processResult(
            PaymentFlowResult.Unvalidated(
                clientSecret = CLIENT_SECRET,
                stripeAccountId = ACCOUNT_ID,
                flowOutcome = StripeIntentResult.Outcome.CANCELED,
                canCancelSource = false,
            )
        ).getOrThrow()

        assertThat(result.outcome).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        repeat(2) { assertRetrieveCall() }
    }

    @Test
    fun `Stripe failure ends polling without success`() = runScenario(
        statuses = listOf(StripeIntent.Status.RequiresAction, StripeIntent.Status.RequiresPaymentMethod)
    ) {
        val result = processor.processResult(RETURN_RESULT).getOrThrow()

        assertThat(result.outcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        repeat(2) { assertRetrieveCall() }
    }

    @Test
    fun `Stripe cancellation ends polling`() = runScenario(
        statuses = listOf(StripeIntent.Status.RequiresAction, StripeIntent.Status.Canceled)
    ) {
        val result = processor.processResult(RETURN_RESULT).getOrThrow()

        assertThat(result.outcome).isEqualTo(StripeIntentResult.Outcome.CANCELED)
        repeat(2) { assertRetrieveCall() }
    }

    @Test
    fun `pending payment times out with a final retrieve and never succeeds`() = runScenario(
        statuses = listOf(StripeIntent.Status.RequiresAction)
    ) {
        val result = processor.processResult(RETURN_RESULT).getOrThrow()

        assertThat(result.outcome).isNotEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        assertThat(scheduler.currentTime).isEqualTo(PaymentFlowResultProcessor.MAX_POLLING_DURATION)
        repeat(TIMEOUT_RETRIEVE_COUNT) { assertRetrieveCall() }
        assertThat(analytics.awaitCall()).isEqualTo(
            FakePollingAnalyticsEventReporter.Call.PollingTimedOut(
                paymentMethodType = "upi",
                lastKnownStatus = "RequiresAction",
                timeLimitSeconds = 15,
            )
        )
    }

    @Test
    fun `processing payment also times out without success`() = runScenario(
        statuses = listOf(StripeIntent.Status.Processing)
    ) {
        val result = processor.processResult(RETURN_RESULT).getOrThrow()

        assertThat(result.outcome).isNotEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
        assertThat(scheduler.currentTime).isEqualTo(PaymentFlowResultProcessor.MAX_POLLING_DURATION)
        repeat(TIMEOUT_RETRIEVE_COUNT) { assertRetrieveCall() }
        assertThat(analytics.awaitCall()).isEqualTo(
            FakePollingAnalyticsEventReporter.Call.PollingTimedOut("upi", "Processing", 15)
        )
    }

    private fun runScenario(statuses: List<StripeIntent.Status>, block: suspend Scenario.() -> Unit) = runTest {
        val repository = FakeStripeRepository(statuses)
        val analytics = FakePollingAnalyticsEventReporter()
        val processor = PaymentIntentFlowResultProcessor(
            ApplicationProvider.getApplicationContext(),
            { ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, null) },
            repository,
            Logger.noop(),
            UnconfinedTestDispatcher(testScheduler),
            analytics,
            Clock { testScheduler.currentTime },
        )
        Scenario(processor, repository, analytics, testScheduler).block()
        repository.calls.ensureAllEventsConsumed()
        analytics.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val processor: PaymentIntentFlowResultProcessor,
        val repository: FakeStripeRepository,
        val analytics: FakePollingAnalyticsEventReporter,
        val scheduler: TestCoroutineScheduler,
    ) {
        suspend fun assertRetrieveCall() {
            val call = repository.calls.awaitItem()
            assertThat(call.clientSecret).isEqualTo(CLIENT_SECRET)
            assertThat(call.options.apiKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
            assertThat(call.options.stripeAccount).isEqualTo(ACCOUNT_ID)
            assertThat(call.expandFields).containsExactly("payment_method")
        }
    }

    private class FakeStripeRepository(statuses: List<StripeIntent.Status>) : AbsFakeStripeRepository() {
        private val responses = ArrayDeque(statuses)
        val calls = Turbine<RetrieveCall>()

        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>,
        ): Result<PaymentIntent> {
            calls.add(RetrieveCall(clientSecret, options, expandFields))
            val status = if (responses.size > 1) responses.removeFirst() else responses.first()
            return Result.success(
                PaymentIntentFixtures.PI_SUCCEEDED.copy(
                    status = status,
                    paymentMethod = PaymentMethod(
                        id = "pm_upi",
                        created = 0L,
                        liveMode = false,
                        type = PaymentMethod.Type.Upi,
                        code = "upi",
                    ),
                    nextActionData = StripeIntent.NextActionData.UpiRedirect("upi://pay?pa=merchant@upi"),
                )
            )
        }

        data class RetrieveCall(
            val clientSecret: String,
            val options: ApiRequest.Options,
            val expandFields: List<String>,
        )
    }

    private companion object {
        const val CLIENT_SECRET = "pi_upi_secret_123"
        const val ACCOUNT_ID = "acct_upi"
        const val TIMEOUT_RETRIEVE_COUNT = 17
        val RETURN_RESULT = PaymentFlowResult.Unvalidated(
            clientSecret = CLIENT_SECRET,
            stripeAccountId = ACCOUNT_ID,
            flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
            canCancelSource = false,
        )
    }
}
