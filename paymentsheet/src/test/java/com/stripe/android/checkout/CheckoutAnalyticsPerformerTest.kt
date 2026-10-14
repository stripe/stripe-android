@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.analytics.CheckoutEventReporterFactory
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.intent.CheckoutSessionResponseKey
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

@RunWith(RobolectricTestRunner::class)
internal class CheckoutAnalyticsPerformerTest {
    @Test
    fun `payment element success reports once and clears saved context`() = runScenario {
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        timeSource += 2.seconds
        performer.reportConfirmationResult(successResult)
        performer.reportConfirmationResult(successResult)

        val call = eventReporter.paymentSuccessCalls.awaitItem()
        assertThat(call.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(call.intentId).isEqualTo(successResult.intent.id)
        assertThat(events().map { it["event"] }).containsExactly(
            "checkout_elements.confirm.started",
            "checkout_elements.confirm.succeeded",
        ).inOrder()
        assertThat(events().last()).containsEntry("duration", 2f)
        assertThat(events().last()).containsEntry("invocation_source", "merchant")
        assertThat(savedStateHandle.keys()).doesNotContain("CheckoutAnalyticsPerformer.paymentSelection")
    }

    @Test
    fun `express checkout context survives recreation without invented duration`() = runScenario {
        val selection = PaymentSelection.Link(brand = LinkBrand.Link)
        performer.onExpressCheckoutElementConfirmationStarted(selection)
        timeSource += 2.seconds
        recreatePerformer().reportConfirmationResult(successResult)

        assertThat(eventReporter.paymentSuccessCalls.awaitItem().paymentSelection).isEqualTo(selection)
        assertThat(events()).hasSize(2)
        assertThat(events().last()).containsEntry("selected_lpm", "link")
        assertThat(events().last()).containsEntry("link_context", "wallet")
        assertThat(events().last()).containsEntry("invocation_source", "express_checkout_element")
        assertThat(events().last()).doesNotContainKey("duration")
    }

    @Test
    fun `express checkout uses its own Link login state`() = runScenario {
        stateHolder.state = stateHolder.state!!.copy(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(linkState = null),
            expressCheckoutElementPaymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = LinkTestUtils.createLinkConfiguration(),
                    loginState = LinkState.LoginState.LoggedIn,
                    signupMode = null,
                ),
            ),
        )

        performer.onExpressCheckoutElementConfirmationStarted(PaymentSelection.Link(brand = LinkBrand.Link))

        assertThat(events().single()).containsEntry("active_link_session", true)
    }

    @Test
    fun `new attempt computes fresh selection and source`() = runScenario {
        performer.onExpressCheckoutElementConfirmationStarted(PaymentSelection.Link(brand = LinkBrand.Link))
        performer.reportConfirmationResult(
            ConfirmationHandler.Result.Canceled(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
        )
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        performer.reportConfirmationResult(successResult)

        eventReporter.paymentSuccessCalls.awaitItem()
        assertThat(events()[2]).containsEntry("selected_lpm", "google_pay")
        assertThat(events()[2]).containsEntry("invocation_source", "merchant")
        assertThat(events()[2]).doesNotContainKey("link_context")
    }

    @Test
    fun `confirmation request id is preserved across later refresh`() = runScenario {
        val initialResponse = CheckoutSessionResponseFactory.create(
            id = "cs_initial_confirmation",
            configId = "cfg_initial_confirmation",
            requestId = "req_initial",
        )
        stateHolder.state = stateHolder.state!!.copy(checkoutSessionResponse = initialResponse)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        val confirmed = CheckoutSessionResponseFactory.create(requestId = "req_confirm")
        stateHolder.state = stateHolder.state!!.copy(
            checkoutSessionResponse = confirmed.copy(
                id = "cs_refreshed_confirmation",
                configId = "cfg_refreshed_confirmation",
                requestId = "req_refresh",
            ),
        )
        performer.reportConfirmationResult(
            successResult.copy(metadata = MutableConfirmationMetadata().apply {
                set(CheckoutSessionResponseKey, confirmed)
            })
        )

        eventReporter.paymentSuccessCalls.awaitItem()
        assertThat(events().first()).containsEntry("checkout_session_id", "cs_initial_confirmation")
        assertThat(events().first()).containsEntry("checkout_config_id", "cfg_initial_confirmation")
        assertThat(events().first()).doesNotContainKey("request_id")
        assertThat(events().last()).containsEntry("checkout_session_id", "cs_refreshed_confirmation")
        assertThat(events().last()).containsEntry("checkout_config_id", "cfg_refreshed_confirmation")
        assertThat(events().last()).containsEntry("request_id", "req_confirm")
    }

    @Test
    fun `refreshed response with null config id preserves initial config id`() = runScenario {
        val initialResponse = CheckoutSessionResponseFactory.create(
            id = "cs_initial_confirmation",
            configId = "cfg_initial_confirmation",
        )
        stateHolder.state = stateHolder.state!!.copy(checkoutSessionResponse = initialResponse)
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)

        val refreshedResponse = CheckoutSessionResponseFactory.create(
            id = "cs_refreshed_confirmation",
            configId = null,
        )
        stateHolder.state = stateHolder.state!!.copy(checkoutSessionResponse = refreshedResponse)
        performer.reportConfirmationResult(successResult)

        eventReporter.paymentSuccessCalls.awaitItem()
        assertThat(events().first()).containsEntry("checkout_config_id", "cfg_initial_confirmation")
        assertThat(events().last()).containsEntry("checkout_session_id", "cs_refreshed_confirmation")
        assertThat(events().last()).containsEntry("checkout_config_id", "cfg_initial_confirmation")
    }

    @Test
    fun `result without confirmation response omits stale request id`() = runScenario {
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        performer.reportConfirmationResult(successResult)

        eventReporter.paymentSuccessCalls.awaitItem()
        assertThat(events().last()).doesNotContainKey("request_id")
    }

    @Test
    fun `startup failure reports safe error and clears context`() = runScenario {
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        performer.reportConfirmationFailure(
            APIException(
                stripeError = StripeError(type = "invalid_request_error", code = "resource_missing"),
                requestId = "req_failed",
                message = "private failure details",
            )
        )
        performer.reportConfirmationFailure(IllegalStateException("duplicate"))

        assertThat(events()).hasSize(2)
        assertThat(events().last()).containsEntry("event", "checkout_elements.confirm.failed")
        assertThat(events().last()).containsEntry("error_type", "invalid_request_error")
        assertThat(events().last()).containsEntry("error_code", "resource_missing")
        assertThat(events().last()).containsEntry("request_id", "req_failed")
        assertThat(events().last().toString()).doesNotContain("private failure details")
    }

    @Test
    fun `buyer cancellation reports canceled without failure fields`() = runScenario {
        performer.onPaymentElementConfirmationStarted(PaymentSelection.GooglePay)
        performer.reportConfirmationResult(
            ConfirmationHandler.Result.Canceled(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
        )

        assertThat(events().last()).containsEntry("event", "checkout_elements.confirm.canceled")
        assertThat(events().last()).doesNotContainKey("error_type")
        assertThat(events().last()).doesNotContainKey("error_code")
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle).apply {
            state = CheckoutControllerStateFactory.create(
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(requestId = "req_stale"),
            )
        }
        val scenario = Scenario(
            eventReporter = FakeEventReporter(),
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            executor = FakeAnalyticsRequestExecutor(),
            timeSource = TestTimeSource(),
        )
        scenario.block()
        scenario.eventReporter.validate()
    }

    private class Scenario(
        val eventReporter: FakeEventReporter,
        val savedStateHandle: SavedStateHandle,
        val stateHolder: CheckoutControllerStateHolder,
        val executor: FakeAnalyticsRequestExecutor,
        val timeSource: TestTimeSource,
    ) {
        val performer = recreatePerformer()

        fun recreatePerformer(): CheckoutAnalyticsPerformer = CheckoutAnalyticsPerformerFactory.create(
            eventReporter = eventReporter,
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            checkoutEventReporter = CheckoutEventReporterFactory.create(executor, timeSource),
        )

        fun events(): List<Map<String, *>> = executor.getExecutedRequests().map { it.params }
    }

    private companion object {
        val successResult = ConfirmationHandler.Result.Succeeded(PaymentIntentFixtures.PI_SUCCEEDED)
    }
}
