package com.stripe.android.checkout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutControllerTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule()

    // The controller defaults the merchant display name to the host app's label, never the checkout
    // session. Mirroring that resolution here keeps the assertion decoupled from Robolectric's
    // package naming while still failing if the code regresses to a session-sourced value.
    private val expectedMerchantDisplayName: String
        get() = applicationContext.applicationInfo
            .loadLabel(applicationContext.packageManager)
            .toString()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `configure returns success`() = runConfigureScenario {
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `configure emits checkoutSession with id from response`() = runConfigureScenario {
        result.getOrThrow()
        assertThat(controller.checkoutSession.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
    }

    @Test
    fun `checkoutSession flow transitions from null to loaded session`() = runTest {
        networkRule.defaultInit()
        val controller = createController()

        controller.checkoutSession.test {
            assertThat(awaitItem()).isNull()

            controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()

            assertThat(awaitItem()?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        }
    }

    @Test
    fun `configure sends adaptive_pricing allowed false by default`() = runConfigureScenario(
        networkSetup = {
            networkRule.checkoutInit(
                bodyPart("adaptive_pricing[allowed]", "false"),
                responseFactory = ::successResponse,
            )
        },
    ) {
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `configure sends adaptive_pricing allowed true when configured`() = runConfigureScenario(
        configuration = CheckoutController.Configuration().adaptivePricingAllowed(true),
        networkSetup = {
            networkRule.checkoutInit(
                bodyPart("adaptive_pricing[allowed]", "true"),
                responseFactory = ::successResponse,
            )
        },
    ) {
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `configure parses session id from client secret`() = runConfigureScenario(
        clientSecret = "cs_test_custom_secret_example",
        networkSetup = {
            // The request must hit the path for the parsed session id, not the response's id.
            networkRule.checkoutInit(sessionId = "cs_test_custom", responseFactory = ::successResponse)
        },
    ) {
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `configure populates confirmationState with payment method metadata`() = runConfigureScenario {
        result.getOrThrow()
        val confirmationState = controller.confirmationStateHolder.state
        assertThat(confirmationState).isNotNull()
        assertThat(confirmationState!!.paymentMethodMetadata).isNotNull()
    }

    @Test
    fun `configure populates controller state with payment method metadata`() = runConfigureScenario {
        result.getOrThrow()
        val controllerState = controller.stateHolder.state
        assertThat(controllerState).isNotNull()
        assertThat(controllerState!!.paymentMethodMetadata).isNotNull()
    }

    @Test
    fun `configure uses app name as merchant display name, not checkout session data`() =
        runConfigureScenario {
            result.getOrThrow()
            assertThat(controller.confirmationStateHolder.state?.configuration?.merchantDisplayName)
                .isEqualTo(expectedMerchantDisplayName)
        }

    @Test
    fun `configure propagates embeddedViewDisplaysMandateText from payment element configuration`() =
        runConfigureScenario(
            configuration = CheckoutController.Configuration().paymentElement(
                PaymentElement.Configuration().embeddedViewDisplaysMandateText(false)
            ),
        ) {
            result.getOrThrow()
            assertThat(controller.confirmationStateHolder.state?.configuration?.embeddedViewDisplaysMandateText)
                .isFalse()
        }

    @Test
    fun `configure succeeds when configuration provides billing email and response omits customer email`() =
        runConfigureScenario(
            configuration = CheckoutController.Configuration().defaultBillingDetails(
                PaymentSheet.BillingDetails(email = "merchant@example.com")
            ),
            networkSetup = {
                networkRule.checkoutInit { response ->
                    response.testBodyFromFile("checkout-session-init.json") { json ->
                        json.getJSONObject("elements_session").remove("link_settings")
                    }
                }
            },
        ) {
            result.getOrThrow()
            assertThat(controller.confirmationStateHolder.state?.configuration?.defaultBillingDetails?.email)
                .isEqualTo("merchant@example.com")
        }

    @Test
    fun `configure returns failure when network request fails`() = runConfigureScenario(
        networkSetup = {
            networkRule.checkoutInit { response ->
                response.setResponseCode(500)
                response.setBody("""{"error": {"message": "Internal server error"}}""")
            }
        },
    ) {
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `configure does not emit checkoutSession when network request fails`() = runConfigureScenario(
        networkSetup = {
            networkRule.checkoutInit { response ->
                response.setResponseCode(500)
                response.setBody("""{"error": {"message": "Internal server error"}}""")
            }
        },
    ) {
        assertThat(result.isFailure).isTrue()
        assertThat(controller.checkoutSession.value).isNull()
        assertThat(controller.confirmationStateHolder.state).isNull()
    }

    @Test
    fun `configure returns failure when response has no elements session`() = runConfigureScenario(
        networkSetup = {
            networkRule.checkoutInit { response ->
                // Identical to the success fixture (customer_email present) except elements_session
                // is removed, so the failure is pinned to the missing session and nothing else.
                response.testBodyFromFile("checkout-session-init.json") { json ->
                    json.put("customer_email", "checkout@example.com")
                    json.remove("elements_session")
                }
            }
        },
    ) {
        assertThat(result.isFailure).isTrue()
        assertThat(controller.confirmationStateHolder.state).isNull()
    }

    @Test
    fun `checkoutSession is null before configure`() = runTest {
        val controller = createController()
        assertThat(controller.checkoutSession.value).isNull()
        assertThat(controller.confirmationStateHolder.state).isNull()
    }

    @Test
    fun `checkoutSession is restored from savedStateHandle after recreation`() = runTest {
        networkRule.defaultInit()
        val savedStateHandle = SavedStateHandle()
        val controller = createController(savedStateHandle)
        controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()

        // Simulate process death: a new controller built from the same saved state.
        val recreated = createController(savedStateHandle)

        assertThat(recreated.checkoutSession.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
    }

    @Test
    fun `confirmationState is restored from savedStateHandle after recreation`() = runTest {
        networkRule.defaultInit()
        val savedStateHandle = SavedStateHandle()
        val controller = createController(savedStateHandle)
        controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()

        val recreated = createController(savedStateHandle)

        val confirmationState = recreated.confirmationStateHolder.state
        assertThat(confirmationState).isNotNull()
        assertThat(confirmationState!!.configuration.merchantDisplayName)
            .isEqualTo(expectedMerchantDisplayName)
    }

    @Test
    fun `callback identifier is generated and stored when absent`() = runTest {
        val savedStateHandle = SavedStateHandle()
        createController(savedStateHandle)

        assertThat(savedStateHandle.get<String>(CALLBACK_IDENTIFIER_KEY)).isNotNull()
    }

    @Test
    fun `callback identifier is reused from savedStateHandle when present`() = runTest {
        val savedStateHandle = SavedStateHandle()
        savedStateHandle[CALLBACK_IDENTIFIER_KEY] = "existing_identifier"

        createController(savedStateHandle)

        assertThat(savedStateHandle.get<String>(CALLBACK_IDENTIFIER_KEY))
            .isEqualTo("existing_identifier")
    }

    private fun NetworkRule.defaultInit() {
        checkoutInit(responseFactory = ::successResponse)
    }

    // The merged loader configuration requires a billing email, which `configure` sources from the
    // session's customer_email. The base fixture omits it, so inject one for the success paths.
    // Link is disabled so the loader doesn't fire a consumer session lookup that's unrelated to
    // what these tests verify.
    private fun successResponse(response: MockResponse) {
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.getJSONObject("elements_session").remove("link_settings")
        }
    }

    private fun createController(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): CheckoutController {
        return CheckoutController.Builder(
            application = applicationContext,
            savedStateHandle = savedStateHandle,
        ).resultCallback {}.build()
    }

    private fun runConfigureScenario(
        clientSecret: String = DEFAULT_CLIENT_SECRET,
        configuration: CheckoutController.Configuration = CheckoutController.Configuration(),
        networkSetup: () -> Unit = { networkRule.defaultInit() },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        networkSetup()
        val controller = createController()
        val result = controller.configure(clientSecret, configuration)
        block(Scenario(controller, result))
    }

    private class Scenario(
        val controller: CheckoutController,
        val result: Result<Unit>,
    )

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val CALLBACK_IDENTIFIER_KEY = "CheckoutController_CallbackIdentifier"
    }
}
