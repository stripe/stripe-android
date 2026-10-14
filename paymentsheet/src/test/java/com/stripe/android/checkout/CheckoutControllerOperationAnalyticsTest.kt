package com.stripe.android.checkout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.analytics.CheckoutAnalyticsOperation
import com.stripe.android.checkout.injection.DaggerCheckoutControllerComponent
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.elements.PaymentElement
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutControllerOperationAnalyticsTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )
    private val destroyControllerRule = CleanupTestRule(CheckoutController::destroy)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.emptyRuleChain()
        .around(destroyControllerRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(CoroutineTestRule(StandardTestDispatcher()))

    @After
    fun clearCallbackReferences() {
        PaymentElementCallbackReferences.clear()
    }

    @Test
    fun `unconfigured promotion code reports started and failed`() = runScenario {
        networkRule.expectOperationFailureEvents(
            operation = CheckoutAnalyticsOperation.ApplyPromotionCode,
            errorType = "IllegalStateException",
        )

        val result = controller.applyPromotionCode("10OFF")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session before it is configured.")
    }

    @Test
    fun `thrown merchant update reports started and failed`() = runScenario(
        state = CheckoutControllerStateFactory.create(),
    ) {
        networkRule.expectOperationFailureEvents(
            operation = CheckoutAnalyticsOperation.RunServerUpdate,
            errorType = "IllegalStateException",
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
        )
        val error = IllegalStateException("Merchant update failed")

        val result = controller.runServerUpdate { throw error }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `shipping allowlist rejection is reported before payment flow admission`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                id = DEFAULT_CHECKOUT_SESSION_ID,
                allowedShippingCountries = listOf("US"),
            ),
        ),
        sheetIsOpen = true,
    ) {
        networkRule.expectOperationFailureEvents(
            operation = CheckoutAnalyticsOperation.UpdateShippingAddress,
            errorType = "IllegalArgumentException",
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
        )

        val result = controller.updateShippingAddress(
            name = null,
            address = CheckoutController.Address().country("DE"),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Country code 'DE' is not in allowedShippingCountries")
    }

    private fun runScenario(
        state: CheckoutControllerState? = null,
        sheetIsOpen: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val parentSavedStateHandle = SavedStateHandle()
        val controllerSavedState = CheckoutControllerSavedState(
            parentHandle = parentSavedStateHandle,
            integrationName = DEFAULT_INTEGRATION_NAME,
        )
        controllerSavedState.handle.getLiveData<CheckoutControllerState>(CheckoutControllerStateHolder.STATE_KEY)
        controllerSavedState.handle.getLiveData<CustomerState>(CustomerStateHolder.SAVED_CUSTOMER)
        state?.let {
            CheckoutControllerStateFactory.createStateHolder(controllerSavedState.handle).state = it
        }
        SheetStateHolder(controllerSavedState.handle).sheetIsOpen = sheetIsOpen

        val controller = destroyControllerRule.track(
            DaggerCheckoutControllerComponent.factory().create(
                application = applicationContext,
                paymentElementCallbackIdentifier = DEFAULT_INTEGRATION_NAME,
                resultCallback = CheckoutController.ResultCallback {},
                rowSelectionBehavior = PaymentElement.RowSelectionBehavior.default(),
                checkoutControllerSavedState = controllerSavedState,
            ).checkoutController
        )

        Scenario(controller).block()
        networkRule.validate()
    }

    private data class Scenario(
        val controller: CheckoutController,
    )

    private fun NetworkRule.expectOperationFailureEvents(
        operation: CheckoutAnalyticsOperation,
        errorType: String,
        sessionId: String? = null,
    ) {
        expectOperationEvent(
            operation = operation,
            phase = "started",
            sessionId = sessionId,
        )
        expectOperationEvent(
            operation = operation,
            phase = "failed",
            sessionId = sessionId,
            errorType = errorType,
        )
    }

    private fun NetworkRule.expectOperationEvent(
        operation: CheckoutAnalyticsOperation,
        phase: String,
        sessionId: String?,
        errorType: String? = null,
    ) {
        val matchers = buildList {
            add(host(ANALYTICS_HOST))
            add(method("GET"))
            add(analyticsPayloadField("event", "checkout_elements.${operation.value}.$phase"))
            sessionId?.let { add(analyticsPayloadField("checkout_session_id", it)) }
            errorType?.let { add(analyticsPayloadField("error_type", it)) }
        }

        enqueue(*matchers.toTypedArray()) { response ->
            response.status = "HTTP/1.1 200 OK"
        }
    }

    private companion object {
        const val DEFAULT_INTEGRATION_NAME = "stripe_checkout_operation_analytics"
        const val ANALYTICS_HOST = "q.stripe.com"
    }
}
