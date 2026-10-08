package com.stripe.android

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.view.PaymentRelayActivity
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestParameterInjector::class)
internal class StripeNextActionTest {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun `payment next action with empty secret returns an error for Activity`() = runScenario {
        stripe.handleNextActionForPayment(activity, "")

        assertError(PaymentController.StripeIntentType.PaymentIntent)
    }

    @Test
    fun `payment next action with empty secret returns an error for Fragment`() = runScenario {
        stripe.handleNextActionForPayment(fragment, "")

        assertError(PaymentController.StripeIntentType.PaymentIntent)
    }

    @Test
    fun `setup next action with empty secret returns an error for Activity`() = runScenario {
        stripe.handleNextActionForSetupIntent(activity, "")

        assertError(PaymentController.StripeIntentType.SetupIntent)
    }

    @Test
    fun `setup next action with empty secret returns an error for Fragment`() = runScenario {
        stripe.handleNextActionForSetupIntent(fragment, "")

        assertError(PaymentController.StripeIntentType.SetupIntent)
    }

    @Test
    fun `payment next action error omits the supplied invalid secret`() = runScenario {
        val clientSecret = "person@example.com"
        stripe.handleNextActionForPayment(activity, clientSecret)

        val error = assertError(PaymentController.StripeIntentType.PaymentIntent)

        assertThat(error.stackTraceToString()).doesNotContain(clientSecret)
    }

    @Test
    fun `setup next action error omits the supplied invalid secret`() = runScenario {
        val clientSecret = "person@example.com_secret_invalid"
        stripe.handleNextActionForSetupIntent(activity, clientSecret)

        val error = assertError(PaymentController.StripeIntentType.SetupIntent)

        assertThat(error.stackTraceToString()).doesNotContain(clientSecret)
    }

    @Test
    fun `next action rejects a secret for the opposite intent type`(
        @TestParameter intentType: PaymentController.StripeIntentType,
        @TestParameter host: Host,
    ) = runScenario {
        val clientSecret = when (intentType) {
            PaymentController.StripeIntentType.PaymentIntent -> {
                SetupIntent.ClientSecret("seti_a1b2c3_secret_x7y8z9").value
            }
            PaymentController.StripeIntentType.SetupIntent -> {
                PaymentIntent.ClientSecret("pi_a1b2c3_secret_x7y8z9").value
            }
        }

        when (intentType) {
            PaymentController.StripeIntentType.PaymentIntent -> when (host) {
                Host.Activity -> stripe.handleNextActionForPayment(activity, clientSecret)
                Host.Fragment -> stripe.handleNextActionForPayment(fragment, clientSecret)
            }
            PaymentController.StripeIntentType.SetupIntent -> when (host) {
                Host.Activity -> stripe.handleNextActionForSetupIntent(activity, clientSecret)
                Host.Fragment -> stripe.handleNextActionForSetupIntent(fragment, clientSecret)
            }
        }

        val error = assertError(intentType)

        assertThat(error.stackTraceToString()).doesNotContain(clientSecret)
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val networkClient = FakeStripeNetworkClient()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val repository = StripeApiRepository(
            context = context,
            publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
            requestSurface = StripeRepository.DEFAULT_REQUEST_SURFACE,
            workContext = testDispatcher,
            stripeNetworkClient = networkClient,
            analyticsRequestExecutor = analyticsRequestExecutor,
            fraudDetectionDataRepository = FakeFraudDetectionDataRepository(null),
        )
        val controller = StripePaymentController(
            context = context,
            publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
            stripeRepository = repository,
            workContext = testDispatcher,
            analyticsRequestExecutor = analyticsRequestExecutor,
            uiContext = testDispatcher,
        )
        val stripe = Stripe(
            stripeRepository = repository,
            paymentController = controller,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            workContext = testDispatcher,
        )
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = activityController.get()
        val fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "next_action").commitNow()
        val callback = FakeApiResultCallback()

        try {
            Scenario(
                stripe = stripe,
                activity = activity,
                fragment = fragment,
                callback = callback,
                testDispatcher = testDispatcher,
            ).block()

            callback.ensureAllEventsConsumed()
            networkClient.requests.expectNoEvents()
            networkClient.requests.ensureAllEventsConsumed()
            assertThat(analyticsRequestExecutor.getExecutedRequests()).isEmpty()
        } finally {
            activityController.pause().stop().destroy()
        }
    }

    private data class Scenario(
        val stripe: Stripe,
        val activity: FragmentActivity,
        val fragment: Fragment,
        val callback: FakeApiResultCallback,
        val testDispatcher: TestDispatcher,
    ) {
        suspend fun assertError(type: PaymentController.StripeIntentType): Exception {
            testDispatcher.scheduler.advanceUntilIdle()

            val startedActivity = requireNotNull(shadowOf(activity).nextStartedActivityForResult)
            assertThat(startedActivity.intent.component?.className)
                .isEqualTo(PaymentRelayActivity::class.java.name)
            val resultData = ActivityScenario.launchActivityForResult<PaymentRelayActivity>(
                startedActivity.intent,
            ).use { relayScenario ->
                requireNotNull(relayScenario.result.resultData)
            }
            assertThat(PaymentFlowResult.Unvalidated.fromIntent(resultData).clientSecret).isNull()

            val handled = when (type) {
                PaymentController.StripeIntentType.PaymentIntent -> stripe.onPaymentResult(
                    StripePaymentController.PAYMENT_REQUEST_CODE,
                    resultData,
                    callback,
                )
                PaymentController.StripeIntentType.SetupIntent -> stripe.onSetupResult(
                    StripePaymentController.SETUP_REQUEST_CODE,
                    resultData,
                    callback,
                )
            }
            assertThat(handled).isTrue()
            testDispatcher.scheduler.advanceUntilIdle()

            val error = callback.errors.awaitItem()
            val intentName = when (type) {
                PaymentController.StripeIntentType.PaymentIntent -> "PaymentIntent"
                PaymentController.StripeIntentType.SetupIntent -> "SetupIntent"
            }
            assertThat(error.message).isEqualTo(
                "Invalid $intentName client secret. " +
                    "Pass the client_secret from the $intentName returned by your server."
            )
            callback.successes.expectNoEvents()
            assertThat(shadowOf(activity).nextStartedActivityForResult).isNull()
            return error
        }
    }

    private class FakeApiResultCallback : ApiResultCallback<StripeModel> {
        val successes = Turbine<StripeModel>()
        val errors = Turbine<Exception>()

        override fun onSuccess(result: StripeModel) {
            successes.add(result)
        }

        override fun onError(e: Exception) {
            errors.add(e)
        }

        fun ensureAllEventsConsumed() {
            successes.ensureAllEventsConsumed()
            errors.ensureAllEventsConsumed()
        }
    }

    private class FakeStripeNetworkClient : StripeNetworkClient {
        val requests = Turbine<StripeRequest>()

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            requests.add(request)
            error("An invalid client secret should not trigger a network request.")
        }

        override suspend fun executeRequestForFile(
            request: StripeRequest,
            outputFile: File,
        ): StripeResponse<File> {
            requests.add(request)
            error("An invalid client secret should not trigger a file request.")
        }
    }

    enum class Host {
        Activity,
        Fragment,
    }
}
