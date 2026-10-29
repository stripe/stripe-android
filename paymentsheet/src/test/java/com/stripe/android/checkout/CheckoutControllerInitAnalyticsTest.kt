package com.stripe.android.checkout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.injection.DaggerCheckoutControllerComponent
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.elements.PaymentElement
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutControllerInitAnalyticsTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )
    private val destroyControllerRule = CleanupTestRule(CheckoutController::destroy)
    private val initStartedEvents = Turbine<Unit>()
    private val initSucceededEvents = Turbine<Unit>()
    private val initFailedEvents = Turbine<Unit>()

    @get:Rule
    val rules: RuleChain = RuleChain.emptyRuleChain()
        .around(destroyControllerRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(application))
        .around(CoroutineTestRule(StandardTestDispatcher()))

    @After
    fun clearCallbackReferences() {
        PaymentElementCallbackReferences.clear()
    }

    @Test
    fun `queued configure starts before waiting and cancellation reports failure without loading`() = runTest {
        val controller = createController()
        configureInitialSession(controller)
        val sessionBeforeConfigure = controller.session.value

        val mutationEntered = Turbine<Unit>()
        val releaseMutation = CompletableDeferred<Unit>()
        val mutationFailure = IllegalStateException("server update failed")
        val mutation = async(Dispatchers.Default) {
            controller.runServerUpdate {
                mutationEntered.add(Unit)
                releaseMutation.await()
                Result.failure(mutationFailure)
            }
        }
        try {
            mutationEntered.awaitItem()
            enqueueInitStarted()
            enqueueInitFailed(CANCELLATION_ERROR_TYPE)

            val configure = async(start = CoroutineStart.UNDISPATCHED) {
                controller.configure(DEFAULT_CLIENT_SECRET)
            }

            initStartedEvents.awaitItem()
            assertThat(configure.isCompleted).isFalse()
            initSucceededEvents.expectNoEvents()

            val cancellation = CancellationException("configure canceled while queued")
            configure.cancel(cancellation)
            configure.join()

            assertThat(configure.isCancelled).isTrue()
            val configureError = runCatching { configure.await() }.exceptionOrNull()
            assertThat(configureError).isInstanceOf(CancellationException::class.java)
            assertThat(configureError).hasMessageThat().isEqualTo(cancellation.message)
            initFailedEvents.awaitItem()
            assertThat(controller.session.value).isEqualTo(sessionBeforeConfigure)

            releaseMutation.complete(Unit)
            val mutationResult = mutation.await()
            assertThat(mutationResult).isInstanceOf(CheckoutController.OperationResult.Failed::class.java)
            assertThat((mutationResult as CheckoutController.OperationResult.Failed).error)
                .isSameInstanceAs(mutationFailure)
            networkRule.validate()
        } finally {
            releaseMutation.complete(Unit)
        }

        mutationEntered.ensureAllEventsConsumed()
        ensureAllAnalyticsEventsConsumed()
    }

    @Test
    fun `queued configure finishes after the mutation releases the coordinator`() = runTest {
        val controller = createController()
        configureInitialSession(controller)

        val mutationEntered = Turbine<Unit>()
        val releaseMutation = CompletableDeferred<Unit>()
        val mutation = async(Dispatchers.Default) {
            controller.runServerUpdate {
                mutationEntered.add(Unit)
                releaseMutation.await()
                Result.success(Unit)
            }
        }
        try {
            mutationEntered.awaitItem()
            val initResponses = Turbine<Unit>()
            // The first response refreshes the session for runServerUpdate; the second is for configure.
            networkRule.checkoutInit(responseFactory = { response ->
                initResponses.add(Unit)
                successInitResponse(response)
            })
            networkRule.checkoutInit(responseFactory = { response ->
                initResponses.add(Unit)
                successInitResponse(response)
            })
            enqueueInitStarted()
            enqueueInitSucceeded()

            val configure = async(start = CoroutineStart.UNDISPATCHED) {
                controller.configure(DEFAULT_CLIENT_SECRET)
            }

            initStartedEvents.awaitItem()
            assertThat(configure.isCompleted).isFalse()
            initResponses.expectNoEvents()
            initSucceededEvents.expectNoEvents()
            initFailedEvents.expectNoEvents()

            releaseMutation.complete(Unit)

            assertThat(mutation.await())
                .isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
            initResponses.awaitItem()
            initResponses.awaitItem()
            assertThat(configure.await())
                .isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
            initSucceededEvents.awaitItem()
            initResponses.ensureAllEventsConsumed()
            assertThat(controller.session.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
            networkRule.validate()
        } finally {
            releaseMutation.complete(Unit)
        }

        mutationEntered.ensureAllEventsConsumed()
        ensureAllAnalyticsEventsConsumed()
    }

    @Test
    fun `missing default billing country throws and reports init failure`() = runTest {
        val controller = createController()
        enqueueInitStarted()
        enqueueInitFailed(INVALID_DEFAULT_ERROR_TYPE)
        val configuration = CheckoutController.Configuration().defaults(
            CheckoutController.Configuration.Defaults().billingDetails(
                CheckoutController.Configuration.Defaults.ContactDetails().address(
                    CheckoutController.Address(),
                ),
            ),
        )

        val error = runCatching {
            controller.configure(DEFAULT_CLIENT_SECRET, configuration)
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat().isEqualTo("Country is required.")
        assertThat(controller.session.value).isNull()
        initStartedEvents.awaitItem()
        initFailedEvents.awaitItem()
        initSucceededEvents.expectNoEvents()
        networkRule.validate()
        ensureAllAnalyticsEventsConsumed()
    }

    private suspend fun configureInitialSession(controller: CheckoutController) {
        networkRule.checkoutInit(responseFactory = ::successInitResponse)
        enqueueInitStarted()
        enqueueInitSucceeded()

        assertThat(controller.configure(DEFAULT_CLIENT_SECRET))
            .isInstanceOf(CheckoutController.OperationResult.Succeeded::class.java)
        assertThat(controller.session.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        initStartedEvents.awaitItem()
        initSucceededEvents.awaitItem()
    }

    private fun createController(): CheckoutController {
        val integrationName = "checkout_init_analytics_test"
        val savedState = CheckoutControllerSavedState(
            parentHandle = SavedStateHandle(),
            integrationName = integrationName,
        )
        return destroyControllerRule.track(
            DaggerCheckoutControllerComponent.factory().create(
                application = application,
                paymentElementCallbackIdentifier = integrationName,
                resultCallback = CheckoutController.ResultCallback {},
                rowSelectionBehavior = PaymentElement.RowSelectionBehavior.default(),
                checkoutControllerSavedState = savedState,
            ).checkoutController,
        )
    }

    private fun enqueueInitStarted() {
        enqueueInitEvent(
            eventName = INIT_STARTED,
            eventSink = initStartedEvents,
            durationMatcher = not(hasQueryParam("duration")),
        )
    }

    private fun enqueueInitSucceeded() {
        enqueueInitEvent(
            eventName = INIT_SUCCEEDED,
            eventSink = initSucceededEvents,
            durationMatcher = hasQueryParam("duration"),
        )
    }

    private fun enqueueInitFailed(errorType: String) {
        enqueueInitEvent(
            eventName = INIT_FAILED,
            eventSink = initFailedEvents,
            durationMatcher = hasQueryParam("duration"),
            errorType = errorType,
        )
    }

    private fun enqueueInitEvent(
        eventName: String,
        eventSink: Turbine<Unit>,
        durationMatcher: RequestMatcher,
        errorType: String? = null,
    ) {
        networkRule.enqueue(
            host("q.stripe.com"),
            method("GET"),
            analyticsPayloadField("event", eventName),
            analyticsPayloadField("product_usage", "checkout_elements"),
            analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
            durationMatcher,
            *listOfNotNull(errorType?.let { analyticsPayloadField("error_type", it) }).toTypedArray(),
        ) { _, response ->
            response.setResponseCode(200)
            eventSink.add(Unit)
        }
    }

    private fun ensureAllAnalyticsEventsConsumed() {
        initStartedEvents.ensureAllEventsConsumed()
        initSucceededEvents.ensureAllEventsConsumed()
        initFailedEvents.ensureAllEventsConsumed()
    }

    private fun successInitResponse(response: MockResponse) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.remove("link_settings")
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val INIT_STARTED = "checkout_elements.init.started"
        const val INIT_SUCCEEDED = "checkout_elements.init.succeeded"
        const val INIT_FAILED = "checkout_elements.init.failed"
        const val CANCELLATION_ERROR_TYPE = "java.util.concurrent.CancellationException"
        const val INVALID_DEFAULT_ERROR_TYPE = "invalidRequestError"
    }
}
