@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.analytics.CheckoutEventReporterTestFactory
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@RunWith(RobolectricTestRunner::class)
internal class CheckoutAnalyticsPerformerTest {
    @Test
    fun `payment element confirmation reports success once and clears saved context`() = runScenario {
        startReporting(performer)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        timeSource += 2.seconds

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)
        confirmationHandler.state.value = ConfirmationHandler.State.Idle
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)
        runCurrent()

        val events = events()
        assertThat(events.map { it["event"] }).containsExactly(
            "checkout_elements.confirm.started",
            "checkout_elements.confirm.succeeded",
        ).inOrder()
        assertThat(events[0]).containsEntry("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID)
        assertThat(events[1]).containsEntry("selected_lpm", "google_pay")
        assertThat(events[1]).containsEntry("invocation_source", "merchant")
        assertThat(events[1]).containsEntry("duration", 2f)
        assertThat(events[1]).doesNotContainKey("request_id")
        assertThat(savedStateHandle.keys().filter { it.startsWith("CheckoutAnalyticsPerformer.") }).isEmpty()
    }

    @Test
    fun `failed confirmation reports safe error fields`() = runScenario {
        startReporting(performer)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        timeSource += 2500.milliseconds
        val privateMessage = "private payment details"
        val error = APIException(
            stripeError = StripeError(
                type = "card_error",
                code = "card_declined",
                message = privateMessage,
            ),
            requestId = "req_failed_confirmation",
            message = privateMessage,
        )

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = error,
                message = privateMessage.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        val params = events().last()
        assertThat(params["event"]).isEqualTo("checkout_elements.confirm.failed")
        assertThat(params).containsEntry("error_type", "card_error")
        assertThat(params).containsEntry("error_code", "card_declined")
        assertThat(params).containsEntry("request_id", "req_failed_confirmation")
        assertThat(params).containsEntry("duration", 2.5f)
        assertThat(params.toString()).doesNotContain(privateMessage)
    }

    @Test
    fun `canceled confirmation reports canceled without error fields`() = runScenario {
        startReporting(performer)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        timeSource += 250.milliseconds

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                ConfirmationHandler.Result.Canceled.Action.InformCancellation
            )
        )

        val params = events().last()
        assertThat(params["event"]).isEqualTo("checkout_elements.confirm.canceled")
        assertThat(params).containsEntry("duration", 0.25f)
        assertThat(params).doesNotContainKey("request_id")
        assertThat(params).doesNotContainKey("error_type")
        assertThat(params).doesNotContainKey("error_code")
    }

    @Test
    fun `express checkout context survives recreation without invented duration`() = runScenario {
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)
        performer.onExpressCheckoutElementConfirmationStarted(selection)
        val restoredPerformer = recreatePerformer()
        startReporting(restoredPerformer)
        timeSource += 2.seconds

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        val events = events()
        assertThat(events).hasSize(2)
        val terminal = events.last()
        assertThat(terminal).containsEntry("event", "checkout_elements.confirm.succeeded")
        assertThat(terminal).containsEntry("selected_lpm", "link")
        assertThat(terminal).containsEntry("link_context", "wallet")
        assertThat(terminal).containsEntry("invocation_source", "express_checkout_element")
        assertThat(terminal).doesNotContainKey("duration")
    }

    @Test
    fun `express checkout uses its own Link session metadata`() = runScenario {
        stateHolder.state = stateHolder.state!!.copy(
            paymentMethodMetadata = linkMetadata(LinkState.LoginState.LoggedIn),
            expressCheckoutElementPaymentMethodMetadata = linkMetadata(LinkState.LoginState.NeedsVerification),
        )

        performer.onExpressCheckoutElementConfirmationStarted(PaymentSelection.Link(brand = LinkBrand.Link))

        val params = events().single()
        assertThat(params).containsEntry("active_link_session", false)
        assertThat(params).containsEntry("link_context", "wallet")
        assertThat(params).containsEntry("invocation_source", "express_checkout_element")
    }

    @Test
    fun `initial config id survives a terminal response without config id`() = runScenario {
        val initialResponse = CheckoutSessionResponseFactory.create(
            id = "cs_initial_confirmation",
            configId = "cfg_initial_confirmation",
        )
        stateHolder.state = stateHolder.state!!.copy(checkoutSessionResponse = initialResponse)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        stateHolder.state = stateHolder.state!!.copy(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                id = "cs_refreshed_confirmation",
                configId = null,
            )
        )
        startReporting(performer)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(successResult)

        val terminal = events().last()
        assertThat(terminal).containsEntry("checkout_session_id", "cs_refreshed_confirmation")
        assertThat(terminal).containsEntry("checkout_config_id", "cfg_initial_confirmation")
    }

    @Test
    fun `confirmation response metadata wins over refreshed state and provides request id`() = runScenario {
        val initialResponse = CheckoutSessionResponseFactory.create(
            id = "cs_initial_confirmation",
            configId = "cfg_initial_confirmation",
        )
        stateHolder.state = stateHolder.state!!.copy(checkoutSessionResponse = initialResponse)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        stateHolder.state = stateHolder.state!!.copy(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                id = "cs_refreshed_confirmation",
                configId = "cfg_refreshed_confirmation",
                requestId = "req_refreshed_state",
            )
        )
        val confirmationResponse = CheckoutSessionResponseFactory.create(
            id = "cs_confirmed",
            configId = "cfg_confirmed",
            requestId = "req_confirmed",
        )
        val resultWithResponse = successResult.copy(
            metadata = MutableConfirmationMetadata().apply {
                set(CheckoutSessionResponseKey, confirmationResponse)
            }
        )
        startReporting(performer)

        confirmationHandler.state.value = ConfirmationHandler.State.Complete(resultWithResponse)

        val terminal = events().last()
        assertThat(terminal).containsEntry("checkout_session_id", "cs_confirmed")
        assertThat(terminal).containsEntry("checkout_config_id", "cfg_confirmed")
        assertThat(terminal).containsEntry("request_id", "req_confirmed")
        assertThat(terminal).doesNotContainValue("req_refreshed_state")
    }

    private fun linkMetadata(loginState: LinkState.LoginState): PaymentMethodMetadata =
        PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = LinkTestUtils.createLinkConfiguration(),
                loginState = loginState,
                signupMode = null,
            )
        )

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle).apply {
            state = CheckoutControllerStateFactory.create(
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    requestId = "req_stale_state"
                )
            )
        }
        val linkAccountHolder = LinkAccountHolder(savedStateHandle)
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val timeSource = TestTimeSource()
        val scenario = Scenario(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            linkAccountHolder = linkAccountHolder,
            analyticsRequestExecutor = analyticsRequestExecutor,
            timeSource = timeSource,
            backgroundScope = backgroundScope,
            testScheduler = testScheduler,
        )

        scenario.block()

        confirmationHandler.validate()
    }

    private class Scenario(
        val confirmationHandler: FakeConfirmationHandler,
        val savedStateHandle: SavedStateHandle,
        val stateHolder: CheckoutControllerStateHolder,
        private val linkAccountHolder: LinkAccountHolder,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val timeSource: TestTimeSource,
        private val backgroundScope: CoroutineScope,
        private val testScheduler: TestCoroutineScheduler,
    ) {
        private val checkoutEventReporter = CheckoutEventReporterTestFactory.create(
            analyticsRequestExecutor = analyticsRequestExecutor,
            timeSource = timeSource,
        )

        val performer = createPerformer()

        fun recreatePerformer(): CheckoutAnalyticsPerformer = createPerformer()

        fun startReporting(performer: CheckoutAnalyticsPerformer) {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                performer.reportConfirmationResults()
            }
        }

        fun runCurrent() {
            testScheduler.runCurrent()
        }

        fun events(): List<Map<String, *>> =
            analyticsRequestExecutor.getExecutedRequests().map { it.params }

        private fun createPerformer(): CheckoutAnalyticsPerformer = CheckoutAnalyticsPerformer(
            confirmationHandler = confirmationHandler,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            linkAccountHolder = linkAccountHolder,
            checkoutEventReporter = checkoutEventReporter,
        )
    }

    private companion object {
        val successResult = ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
    }
}
