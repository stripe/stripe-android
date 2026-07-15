package com.stripe.android.checkout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
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

    // Destroys built controllers when the test finishes, releasing each one's viewModelScope.
    private val destroyControllerRule = CleanupTestRule(CheckoutController::destroy)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.emptyRuleChain()
        .around(destroyControllerRule)
        .around(networkRule)
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
    fun `configure populates state with payment method metadata`() = runConfigureScenario {
        result.getOrThrow()
        val state = committedState
        assertThat(state).isNotNull()
        assertThat(state!!.paymentMethodMetadata).isNotNull()
    }

    @Test
    fun `configure uses app name as merchant display name, not checkout session data`() =
        runConfigureScenario {
            result.getOrThrow()
            assertThat(committedState?.embeddedConfiguration?.merchantDisplayName)
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
            assertThat(committedState?.embeddedConfiguration?.embeddedViewDisplaysMandateText)
                .isFalse()
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
        assertThat(committedState).isNull()
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
        assertThat(committedState).isNull()
    }

    @Test
    fun `checkoutSession is null before configure`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val controller = createController(savedStateHandle)
        assertThat(controller.checkoutSession.value).isNull()
        assertThat(CheckoutControllerStateHolder(savedStateHandle, FakeErrorReporter()).state).isNull()
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
    fun `state is restored from savedStateHandle after recreation`() = runTest {
        networkRule.defaultInit()
        val savedStateHandle = SavedStateHandle()
        val controller = createController(savedStateHandle)
        controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()

        // A fresh state holder over the same SavedStateHandle simulates the controller being
        // rebuilt after process death: the committed state is read back from persisted storage.
        val state = CheckoutControllerStateHolder(savedStateHandle, FakeErrorReporter()).state
        assertThat(state).isNotNull()
        assertThat(state!!.embeddedConfiguration.merchantDisplayName)
            .isEqualTo(expectedMerchantDisplayName)
    }

    @Test
    fun `destroy clears the committed state`() = runConfigureScenario {
        result.getOrThrow()
        // Pre-condition: configure committed a non-null state so the clear is observable.
        assertThat(committedState).isNotNull()

        controller.destroy()

        assertThat(committedState).isNull()
        assertThat(controller.checkoutSession.value).isNull()
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

    @Test
    fun `applyPromotionCode sends promotion code and reloads on success`() = runMutationScenario {
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "10OFF"),
            responseFactory = successResponseFactory(),
        )

        val result = controller.applyPromotionCode("10OFF")

        result.getOrThrow()
        assertThat(committedState().paymentMethodMetadata).isNotNull()
    }

    @Test
    fun `applyPromotionCode trims whitespace from promotion code`() = runMutationScenario {
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "10OFF"),
            responseFactory = successResponseFactory(),
        )

        val result = controller.applyPromotionCode("  10OFF  ")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `applyPromotionCode returns failure and preserves session on error`() = runMutationScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid promotion code"}}""")
        }
        val before = controller.checkoutSession.value

        val result = controller.applyPromotionCode("INVALID")

        assertThat(result.isFailure).isTrue()
        assertThat(controller.checkoutSession.value).isEqualTo(before)
    }

    @Test
    fun `removePromotionCode sends empty promotion code on success`() = runMutationScenario {
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", ""),
            responseFactory = successResponseFactory(),
        )

        val result = controller.removePromotionCode()

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `updateLineItemQuantity sends line item params and updates session on success`() = runMutationScenario {
        networkRule.checkoutUpdate(
            bodyPart("updated_line_item_quantity[line_item_id]", "li_1"),
            bodyPart("updated_line_item_quantity[quantity]", "3"),
            bodyPart("updated_line_item_quantity[fail_update_on_discount_error]", "true"),
            responseFactory = successResponseFactory { json ->
                json.put("total_summary", totalSummaryJson(due = 7000))
            },
        )

        val result = controller.updateLineItemQuantity("li_1", 3)

        result.getOrThrow()
        assertThat(controller.checkoutSession.value?.totalSummary?.totalDueToday).isEqualTo(7000)
    }

    @Test
    fun `updateLineItemQuantity returns failure and preserves session on error`() = runMutationScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid quantity"}}""")
        }
        val before = controller.checkoutSession.value

        val result = controller.updateLineItemQuantity("li_1", -1)

        assertThat(result.isFailure).isTrue()
        assertThat(controller.checkoutSession.value).isEqualTo(before)
    }

    @Test
    fun `selectShippingOption sends shipping rate on success`() = runMutationScenario {
        networkRule.checkoutUpdate(
            bodyPart("shipping_rate", "shr_express"),
            bodyPart("elements_session_client[is_aggregation_expected]", "true"),
            responseFactory = successResponseFactory(),
        )

        val result = controller.selectShippingOption("shr_express")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `selectShippingOption returns failure on error`() = runMutationScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid shipping rate"}}""")
        }
        val before = controller.checkoutSession.value

        val result = controller.selectShippingOption("shr_invalid")

        assertThat(result.isFailure).isTrue()
        assertThat(controller.checkoutSession.value).isEqualTo(before)
    }

    @Test
    fun `updateTaxId sends type and value on success`() = runMutationScenario {
        networkRule.checkoutUpdate(
            bodyPart("tax_id_collection[tax_id][type]", "us_ein"),
            bodyPart("tax_id_collection[tax_id][value]", "123456789"),
            bodyPart("elements_session_client[is_aggregation_expected]", "true"),
            responseFactory = successResponseFactory(),
        )

        val result = controller.updateTaxId("us_ein", "123456789")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `updateTaxId trims whitespace from type and value`() = runMutationScenario {
        networkRule.checkoutUpdate(
            bodyPart("tax_id_collection[tax_id][type]", "us_ein"),
            bodyPart("tax_id_collection[tax_id][value]", "123456789"),
            responseFactory = successResponseFactory(),
        )

        val result = controller.updateTaxId("  us_ein  ", "  123456789  ")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `updateShippingAddress sends tax_region and stores address when automatic tax targets shipping`() =
        runMutationScenario(initModifier = automaticTaxFor("shipping")) {
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[city]", "Denver"),
                bodyPart("tax_region[state]", "CO"),
                bodyPart("tax_region[postal_code]", "80202"),
                bodyPart("tax_region[line1]", "123 Main St"),
                bodyPart("tax_region[line2]", "Apt 4"),
                bodyPart("elements_session_client[is_aggregation_expected]", "true"),
                responseFactory = successResponseFactory(automaticTaxFor("shipping")),
            )

            val result = controller.updateShippingAddress(
                name = "John",
                phoneNumber = "5551234567",
                address = fullAddress,
            )

            result.getOrThrow()
            val state = committedState()
            assertThat(state.shippingName).isEqualTo("John")
            assertThat(state.shippingPhoneNumber).isEqualTo("5551234567")
            assertThat(state.shippingAddress).isEqualTo(fullAddress.build())
        }

    @Test
    fun `updateShippingAddress omits empty fields from tax_region request`() =
        runMutationScenario(initModifier = automaticTaxFor("shipping")) {
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[postal_code]", "80202"),
                not(hasBodyPart("tax_region[city]")),
                not(hasBodyPart("tax_region[state]")),
                not(hasBodyPart("tax_region[line1]")),
                not(hasBodyPart("tax_region[line2]")),
                responseFactory = successResponseFactory(automaticTaxFor("shipping")),
            )

            val address = Address().country("US").postalCode("80202")
            val result = controller.updateShippingAddress(name = null, phoneNumber = null, address = address)

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `updateShippingAddress stores address without a network call when automatic tax is disabled`() =
        runMutationScenario {
            // No checkoutUpdate is enqueued: with automatic tax off, the address is stored locally
            // and the payment element is reloaded from the existing response, firing no request.
            val result = controller.updateShippingAddress(name = "John", phoneNumber = null, address = fullAddress)

            result.getOrThrow()
            val state = committedState()
            assertThat(state.shippingName).isEqualTo("John")
            assertThat(state.shippingAddress).isEqualTo(fullAddress.build())
        }

    @Test
    fun `updateShippingAddress does not store address on failure`() =
        runMutationScenario(initModifier = automaticTaxFor("shipping")) {
            networkRule.checkoutUpdate { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid address"}}""")
            }

            val result = controller.updateShippingAddress(name = "John", phoneNumber = null, address = fullAddress)

            assertThat(result.isFailure).isTrue()
            val state = committedState()
            assertThat(state.shippingName).isNull()
            assertThat(state.shippingAddress).isNull()
        }

    @Test
    fun `updateBillingAddress sends tax_region and stores address when automatic tax targets billing`() =
        runMutationScenario(initModifier = automaticTaxFor("billing")) {
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[city]", "Denver"),
                bodyPart("tax_region[postal_code]", "80202"),
                bodyPart("elements_session_client[is_aggregation_expected]", "true"),
                responseFactory = successResponseFactory(automaticTaxFor("billing")),
            )

            val result = controller.updateBillingAddress(
                name = "Jane",
                phoneNumber = "5559876543",
                address = fullAddress,
            )

            result.getOrThrow()
            val state = committedState()
            assertThat(state.billingName).isEqualTo("Jane")
            assertThat(state.billingPhoneNumber).isEqualTo("5559876543")
            assertThat(state.billingAddress).isEqualTo(fullAddress.build())
        }

    @Test
    fun `updateBillingAddress does not send tax_region when automatic tax targets shipping`() =
        runMutationScenario(initModifier = automaticTaxFor("shipping")) {
            // Automatic tax targets shipping, so a billing address update stays local: no request.
            val result = controller.updateBillingAddress(name = "Jane", phoneNumber = null, address = fullAddress)

            result.getOrThrow()
            val state = committedState()
            assertThat(state.billingName).isEqualTo("Jane")
            assertThat(state.billingAddress).isEqualTo(fullAddress.build())
        }

    @Test
    fun `runServerUpdate refreshes the session after serverUpdate completes`() = runMutationScenario {
        networkRule.checkoutInit(
            responseFactory = successResponseFactory { json ->
                json.put("total_summary", totalSummaryJson(due = 8000))
            },
        )

        val result = controller.runServerUpdate { Result.success(Unit) }

        result.getOrThrow()
        assertThat(controller.checkoutSession.value?.totalSummary?.totalDueToday).isEqualTo(8000)
    }

    @Test
    fun `runServerUpdate returns failure when serverUpdate throws`() = runMutationScenario {
        val before = controller.checkoutSession.value

        val result = controller.runServerUpdate { throw IllegalStateException("Server error") }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("Server error")
        assertThat(controller.checkoutSession.value).isEqualTo(before)
    }

    @Test
    fun `runServerUpdate returns failure when serverUpdate fails`() = runMutationScenario {
        val result = controller.runServerUpdate {
            Result.failure(IllegalStateException("Server error"))
        }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("Server error")
    }

    @Test
    fun `runServerUpdate returns failure when the refresh fails`() = runMutationScenario {
        networkRule.checkoutInit { response ->
            response.setResponseCode(500)
            response.setBody("""{"error": {"message": "Internal server error"}}""")
        }
        val before = controller.checkoutSession.value

        val result = controller.runServerUpdate { Result.success(Unit) }

        assertThat(result.isFailure).isTrue()
        assertThat(controller.checkoutSession.value).isEqualTo(before)
    }

    @Test
    fun `runServerUpdate returns failure when serverUpdate exceeds the timeout`() = runMutationScenario {
        val result = controller.runServerUpdate {
            delay(21_000)
            Result.success(Unit)
        }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(TimeoutCancellationException::class.java)
    }

    @Test
    fun `mutation returns failure before the session is configured`() = runTest {
        val controller = createController()

        val result = controller.applyPromotionCode("10OFF")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session before it is configured.")
    }

    @Test
    fun `mutation returns failure when a payment flow is presented`() = runMutationScenario {
        markIntegrationLaunched()

        val result = controller.applyPromotionCode("10OFF")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session while a payment flow is presented.")
    }

    @Test
    fun `runServerUpdate returns failure when a payment flow is presented`() = runMutationScenario {
        markIntegrationLaunched()

        val result = controller.runServerUpdate { Result.success(Unit) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session while a payment flow is presented.")
    }

    @Test
    fun `isLoading transitions to true then false on a successful mutation`() = runMutationScenario(
        assertLoadingConsumed = true,
    ) {
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "10OFF"),
            responseFactory = successResponseFactory(),
        )

        assertThat(isLoadingTurbine.awaitItem()).isFalse()

        controller.applyPromotionCode("10OFF")

        assertThat(isLoadingTurbine.awaitItem()).isTrue()
        assertThat(isLoadingTurbine.awaitItem()).isFalse()
    }

    @Test
    fun `isLoading stays true while queued mutations are pending`() = runMutationScenario(
        assertLoadingConsumed = true,
    ) {
        val holdFirstResponse = CountDownLatch(1)
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "10OFF"),
        ) { response ->
            holdFirstResponse.await(10, TimeUnit.SECONDS)
            successResponseFactory().invoke(response)
        }
        networkRule.checkoutUpdate(
            bodyPart("promotion_code", "20OFF"),
            responseFactory = successResponseFactory(),
        )

        assertThat(isLoadingTurbine.awaitItem()).isFalse()

        val job1 = async { controller.applyPromotionCode("10OFF") }
        val job2 = async { controller.applyPromotionCode("20OFF") }
        testScheduler.advanceUntilIdle()

        assertThat(isLoadingTurbine.awaitItem()).isTrue()

        holdFirstResponse.countDown()
        job1.await()
        job2.await()

        // isLoading should go directly from true to false with no intermediate flicker.
        assertThat(isLoadingTurbine.awaitItem()).isFalse()
    }

    @Test
    fun `concurrent mutations are serialized so the second uses the first result's session id`() =
        runMutationScenario {
            // The first mutation is delayed and returns a new session id.
            networkRule.checkoutUpdate(
                bodyPart("promotion_code", "10OFF"),
            ) { response ->
                response.setBodyDelay(200, TimeUnit.MILLISECONDS)
                successResponseFactory { json -> json.put("session_id", "cs_test_after_promo") }.invoke(response)
            }
            // The second mutation must target the session id produced by the first, proving the
            // mutex serialized them (an unserialized call would hit the original id and fail).
            networkRule.checkoutUpdate(
                bodyPart("updated_line_item_quantity[line_item_id]", "li_1"),
                sessionId = "cs_test_after_promo",
                responseFactory = successResponseFactory { json -> json.put("session_id", "cs_test_after_promo") },
            )

            val results = listOf(
                async { controller.applyPromotionCode("10OFF") },
                async { controller.updateLineItemQuantity("li_1", 2) },
            ).awaitAll()

            assertThat(results[0].isSuccess).isTrue()
            assertThat(results[1].isSuccess).isTrue()
            assertThat(controller.checkoutSession.value?.id).isEqualTo("cs_test_after_promo")
        }

    @Test
    fun `configure toggles isLoading true then false`() = runTest {
        networkRule.defaultInit()
        val controller = createController()

        turbineScope {
            val isLoadingTurbine = controller.isLoading.testIn(backgroundScope)
            assertThat(isLoadingTurbine.awaitItem()).isFalse()

            controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()

            assertThat(isLoadingTurbine.awaitItem()).isTrue()
            assertThat(isLoadingTurbine.awaitItem()).isFalse()
            isLoadingTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `configure resets isLoading to false when the request fails`() = runTest {
        networkRule.checkoutInit { response ->
            response.setResponseCode(500)
            response.setBody("""{"error": {"message": "Internal server error"}}""")
        }
        val controller = createController()

        turbineScope {
            val isLoadingTurbine = controller.isLoading.testIn(backgroundScope)
            assertThat(isLoadingTurbine.awaitItem()).isFalse()

            assertThat(controller.configure(DEFAULT_CLIENT_SECRET).isFailure).isTrue()

            // The failure path must still release the loading window via the finally block.
            assertThat(isLoadingTurbine.awaitItem()).isTrue()
            assertThat(isLoadingTurbine.awaitItem()).isFalse()
        }
    }

    @Test
    fun `configure is serialized behind an in-flight mutation and shares its loading window`() =
        runMutationScenario(assertLoadingConsumed = true) {
            val holdMutation = CountDownLatch(1)
            networkRule.checkoutUpdate(
                bodyPart("promotion_code", "10OFF"),
            ) { response ->
                holdMutation.await(10, TimeUnit.SECONDS)
                successResponseFactory().invoke(response)
            }
            networkRule.checkoutInit(responseFactory = successResponseFactory())

            assertThat(isLoadingTurbine.awaitItem()).isFalse()

            val mutation = async { controller.applyPromotionCode("10OFF") }
            val configure = async { controller.configure(DEFAULT_CLIENT_SECRET) }
            testScheduler.advanceUntilIdle()

            assertThat(isLoadingTurbine.awaitItem()).isTrue()
            // configure cannot complete while the mutation holds the mutex, proving it is serialized.
            assertThat(configure.isCompleted).isFalse()

            holdMutation.countDown()
            assertThat(mutation.await().isSuccess).isTrue()
            assertThat(configure.await().isSuccess).isTrue()

            // A single loading window spanned both operations, with no flicker to false in between.
            assertThat(isLoadingTurbine.awaitItem()).isFalse()
        }

    // region allowedShippingCountries validation

    @Test
    fun `updateShippingAddress succeeds when country is in allowedShippingCountries`() =
        runMutationScenario(
            initModifier = combine(allowedShippingCountries(listOf("US", "CA")), automaticTaxFor("shipping")),
        ) {
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("elements_session_client[is_aggregation_expected]", "true"),
                responseFactory = successResponseFactory(
                    combine(allowedShippingCountries(listOf("US", "CA")), automaticTaxFor("shipping")),
                ),
            )

            val result = controller.updateShippingAddress(
                name = null,
                phoneNumber = null,
                address = Address().country("US"),
            )

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `updateShippingAddress fails with IllegalArgumentException for disallowed country`() =
        runMutationScenario(
            initModifier = allowedShippingCountries(listOf("US", "CA")),
            assertLoadingConsumed = true,
        ) {
            val before = controller.checkoutSession.value

            // Fast-fail returns before runSerialized, so isLoading must never flip to true.
            assertThat(isLoadingTurbine.awaitItem()).isFalse()

            val result = controller.updateShippingAddress(
                name = null,
                phoneNumber = null,
                address = Address().country("DE"),
            )

            assertThat(result.isFailure).isTrue()
            val exception = result.exceptionOrNull()
            assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(exception).hasMessageThat().isEqualTo(
                "Country code 'DE' is not in allowedShippingCountries"
            )
            assertThat(controller.checkoutSession.value).isEqualTo(before)
        }

    @Test
    fun `updateShippingAddress succeeds when allowedShippingCountries is null`() =
        runMutationScenario {
            // No allowlist set, so any country passes.
            val result = controller.updateShippingAddress(
                name = null,
                phoneNumber = null,
                address = Address().country("DE"),
            )

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `updateShippingAddress fails for all countries when allowedShippingCountries is empty`() =
        runMutationScenario(initModifier = allowedShippingCountries(emptyList())) {
            val before = controller.checkoutSession.value

            val result = controller.updateShippingAddress(
                name = null,
                phoneNumber = null,
                address = Address().country("US"),
            )

            assertThat(result.isFailure).isTrue()
            val exception = result.exceptionOrNull()
            assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(exception).hasMessageThat().isEqualTo(
                "Country code 'US' is not in allowedShippingCountries"
            )
            assertThat(controller.checkoutSession.value).isEqualTo(before)
        }

    @Test
    fun `updateBillingAddress is not gated by allowedShippingCountries`() =
        runMutationScenario(initModifier = allowedShippingCountries(listOf("US"))) {
            val result = controller.updateBillingAddress(
                name = null,
                phoneNumber = null,
                address = Address().country("DE"),
            )

            assertThat(result.isSuccess).isTrue()
        }

    // endregion

    private fun NetworkRule.defaultInit() {
        checkoutInit(responseFactory = ::successResponse)
    }

    // The merged loader configuration requires a billing email, which `configure` sources from the
    // session's customer_email. The base fixture omits it, so inject one for the success paths.
    // Link is disabled so the loader doesn't fire a consumer session lookup that's unrelated to
    // what these tests verify.
    private fun successResponse(response: MockResponse) {
        successResponseFactory().invoke(response)
    }

    // Builds an init-style success response. Because every mutation reloads the payment element,
    // mutation responses must also carry a full elements_session, so the same builder is reused for
    // both `configure` and mutation endpoints. [jsonModifier] tweaks the body per test.
    private fun successResponseFactory(
        jsonModifier: (JSONObject) -> Unit = {},
    ): (MockResponse) -> Unit = { response ->
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.getJSONObject("elements_session").remove("link_settings")
            jsonModifier(json)
        }
    }

    // Builds a total_summary object. The parser requires subtotal, due, and total to all be present
    // to produce a non-null summary, so a test asserting on totalDueToday must set all three.
    private fun totalSummaryJson(due: Long): JSONObject = JSONObject()
        .put("subtotal", due)
        .put("due", due)
        .put("total", due)

    private fun combine(vararg modifiers: (JSONObject) -> Unit): (JSONObject) -> Unit = { json ->
        modifiers.forEach { it(json) }
    }

    // Sets shipping_address_collection.allowed_countries in the session JSON.
    private fun allowedShippingCountries(countries: List<String>): (JSONObject) -> Unit = { json ->
        val countriesArray = JSONArray().apply { countries.forEach { put(it) } }
        json.put(
            "shipping_address_collection",
            JSONObject().put("allowed_countries", countriesArray),
        )
    }

    // Enables automatic tax with the given address source ("shipping" or "billing"), so an address
    // update sends tax_region to the server.
    private fun automaticTaxFor(source: String): (JSONObject) -> Unit = { json ->
        json.put(
            "tax_context",
            JSONObject()
                .put("automatic_tax_enabled", true)
                .put("automatic_tax_address_source", source),
        )
    }

    private fun createController(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): CheckoutController {
        return destroyControllerRule.track(
            CheckoutController.Builder(
                application = applicationContext,
                savedStateHandle = savedStateHandle,
            ).build()
        )
    }

    private fun runConfigureScenario(
        clientSecret: String = DEFAULT_CLIENT_SECRET,
        configuration: CheckoutController.Configuration = CheckoutController.Configuration(),
        networkSetup: () -> Unit = { networkRule.defaultInit() },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        networkSetup()
        val savedStateHandle = SavedStateHandle()
        val controller = createController(savedStateHandle)
        val result = controller.configure(clientSecret, configuration)
        block(Scenario(controller, result, savedStateHandle))
    }

    private class Scenario(
        val controller: CheckoutController,
        val result: Result<Unit>,
        private val savedStateHandle: SavedStateHandle,
    ) {
        // Reads the state the controller committed via its state holder, which shares this
        // SavedStateHandle in the production graph.
        val committedState: CheckoutControllerState?
            get() = CheckoutControllerStateHolder(savedStateHandle, FakeErrorReporter()).state
    }

    // Configures a controller from a fresh init, then hands it to [block] alongside the shared
    // SavedStateHandle (used to read committed state and simulate a presented payment flow) and an
    // isLoading Turbine.
    //
    // Set [assertLoadingConsumed] for tests that verify loading behavior: the block must consume
    // every isLoading emission and this asserts none are left over. Tests that don't care about
    // loading leave it false, and any unconsumed emissions are ignored.
    private fun runMutationScenario(
        initModifier: (JSONObject) -> Unit = {},
        assertLoadingConsumed: Boolean = false,
        block: suspend MutationScenario.() -> Unit,
    ) = runTest {
        networkRule.checkoutInit(responseFactory = successResponseFactory(initModifier))
        val savedStateHandle = SavedStateHandle()
        val controller = createController(savedStateHandle)
        controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()

        turbineScope {
            val isLoadingTurbine = controller.isLoading.testIn(backgroundScope)
            block(
                MutationScenario(
                    controller = controller,
                    savedStateHandle = savedStateHandle,
                    testScope = this@runTest,
                    isLoadingTurbine = isLoadingTurbine,
                )
            )
            if (assertLoadingConsumed) {
                isLoadingTurbine.ensureAllEventsConsumed()
            } else {
                isLoadingTurbine.cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private class MutationScenario(
        val controller: CheckoutController,
        private val savedStateHandle: SavedStateHandle,
        private val testScope: TestScope,
        val isLoadingTurbine: ReceiveTurbine<Boolean>,
    ) : CoroutineScope by testScope {
        val testScheduler: TestCoroutineScheduler get() = testScope.testScheduler

        val fullAddress: Address = Address()
            .city("Denver")
            .country("US")
            .line1("123 Main St")
            .line2("Apt 4")
            .postalCode("80202")
            .state("CO")

        // Reads the state the controller committed via its state holder, which shares this
        // SavedStateHandle in the production graph.
        fun committedState(): CheckoutControllerState =
            requireNotNull(CheckoutControllerStateHolder(savedStateHandle, FakeErrorReporter()).state)

        // Simulates a presented payment flow by flipping the committed state's integrationLaunched
        // flag, which the mutation guard reads back through the same SavedStateHandle.
        fun markIntegrationLaunched() {
            val stateHolder = CheckoutControllerStateHolder(savedStateHandle, FakeErrorReporter())
            stateHolder.state = committedState().copy(integrationLaunched = true)
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val CALLBACK_IDENTIFIER_KEY = "CheckoutController_CallbackIdentifier"
    }
}
