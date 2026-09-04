package com.stripe.android.checkout

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.checkout.injection.DaggerCheckoutControllerComponent
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.elements.CurrencySelectorElement
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.ece.ExpressButtonType
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.utils.simulateProcessDeath
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
import org.junit.After
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

    private val expectedMerchantDisplayName = "Mobile Example Account"

    // Destroys built controllers when the test finishes, releasing each one's viewModelScope.
    private val destroyControllerRule = CleanupTestRule(CheckoutController::destroy)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.emptyRuleChain()
        .around(destroyControllerRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    // The controller resolves callbacks from the process-global PaymentElementCallbackReferences,
    // keyed by integration name. Clear it between tests so registrations don't leak across cases.
    @After
    fun clearCallbackReferences() {
        PaymentElementCallbackReferences.clear()
    }

    @Test
    fun `configure returns success`() = runConfigureScenario {
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `configure emits session with id from response`() = runConfigureScenario {
        result.getOrThrow()
        assertThat(controller.session.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
    }

    @Test
    fun `session flow transitions from null to loaded session`() = runTest {
        networkRule.defaultInit()
        val controller = createController()

        controller.session.test {
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
        configuration = CheckoutController.Configuration().currencySelectorElement(
            CurrencySelectorElement.Configuration(),
        ),
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
    fun `configure prefills the default billing address`() = runConfigureScenario(
        configuration = CheckoutController.Configuration().defaults(
            CheckoutController.Configuration.Defaults().billingDetails(
                CheckoutController.Configuration.Defaults.ContactDetails().address(
                    CheckoutController.Address()
                        .city(" San Francisco ")
                        .country(" US ")
                        .line1(" 510 Townsend St ")
                        .postalCode(" 94103 ")
                        .state(" CA ")
                )
            )
        ),
    ) {
        result.getOrThrow()

        val billingAddress = requireNotNull(committedState?.embeddedConfiguration?.defaultBillingDetails?.address)
        assertThat(billingAddress.city).isEqualTo("San Francisco")
        assertThat(billingAddress.country).isEqualTo("US")
        assertThat(billingAddress.line1).isEqualTo("510 Townsend St")
        assertThat(billingAddress.postalCode).isEqualTo("94103")
        assertThat(billingAddress.state).isEqualTo("CA")
    }

    @Test
    fun `configure sends default billing address when automatic tax targets billing`() = runConfigureScenario(
        configuration = CheckoutController.Configuration().defaults(
            CheckoutController.Configuration.Defaults().billingDetails(
                CheckoutController.Configuration.Defaults.ContactDetails().address(
                    CheckoutController.Address()
                        .city("San Francisco")
                        .country("US")
                        .line1("510 Townsend St")
                        .line2("Suite 100")
                        .postalCode("94103")
                        .state("CA")
                )
            )
        ),
        networkSetup = {
            networkRule.checkoutInit(
                responseFactory = successResponseFactory(automaticTaxFor("billing")),
            )
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[city]", "San Francisco"),
                bodyPart("tax_region[state]", "CA"),
                bodyPart("tax_region[postal_code]", "94103"),
                bodyPart("tax_region[line1]", "510 Townsend St"),
                bodyPart("tax_region[line2]", "Suite 100"),
                bodyPart("elements_session_client[is_aggregation_expected]", "true"),
                responseFactory = successResponseFactory(automaticTaxFor("billing")),
            )
        },
    ) {
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `configure seeds the default email locally`() = runConfigureScenario(
        configuration = CheckoutController.Configuration().defaults(
            CheckoutController.Configuration.Defaults().email("prefill@example.com")
        ),
        networkSetup = {
            networkRule.checkoutInit(responseFactory = ::successResponse)
        },
    ) {
        result.getOrThrow()
        assertThat(controller.session.value?.email).isEqualTo("prefill@example.com")
        assertThat(committedState?.embeddedConfiguration?.defaultBillingDetails?.email)
            .isEqualTo("prefill@example.com")
    }

    @Test
    fun `configure does not send an email update when the default email is blank`() = runConfigureScenario(
        configuration = CheckoutController.Configuration().defaults(
            CheckoutController.Configuration.Defaults().email("   ")
        ),
    ) {
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `configure defaults merchant display name to the checkout session business name`() =
        runConfigureScenario {
            result.getOrThrow()
            assertThat(committedState?.embeddedConfiguration?.merchantDisplayName)
                .isEqualTo(expectedMerchantDisplayName)
        }

    @Test
    fun `configure uses the configured merchant display name over the checkout session business name`() =
        runConfigureScenario(
            configuration = CheckoutController.Configuration().merchantDisplayName("Acme Corp"),
        ) {
            result.getOrThrow()
            assertThat(committedState?.embeddedConfiguration?.merchantDisplayName)
                .isEqualTo("Acme Corp")
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
    fun `configure upgrades Automatic to Full when session requires billing address`() =
        runConfigureScenario(
            configuration = CheckoutController.Configuration().paymentElement(
                PaymentElement.Configuration()
            ),
            networkSetup = {
                networkRule.checkoutInit(
                    responseFactory = successResponseFactory { json ->
                        json.put("billing_address_collection", "required")
                    },
                )
            },
        ) {
            result.getOrThrow()
            assertThat(committedState?.embeddedConfiguration?.billingDetailsCollectionConfiguration?.address)
                .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
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
    fun `configure does not emit session when network request fails`() = runConfigureScenario(
        networkSetup = {
            networkRule.checkoutInit { response ->
                response.setResponseCode(500)
                response.setBody("""{"error": {"message": "Internal server error"}}""")
            }
        },
    ) {
        assertThat(result.isFailure).isTrue()
        assertThat(controller.session.value).isNull()
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
    fun `session is null before configure`() = runTest {
        val setup = createControllerSetup(SavedStateHandle(), DEFAULT_INTEGRATION_NAME)
        assertThat(setup.controller.session.value).isNull()
        assertThat(setup.stateHolder.state).isNull()
    }

    @Test
    fun `session exposes available express checkout payment methods`() {
        val session = createSession(
            availableExpressButtonTypes = listOf(
                ExpressButtonType.GooglePay(
                    ExpressCheckoutElement.Configuration.GooglePayConfiguration().build()
                ),
                ExpressButtonType.Link,
            ),
        )

        assertThat(session.availableExpressCheckoutPaymentMethods).hasSize(2)
        assertThat(session.availableExpressCheckoutPaymentMethods[0])
            .isInstanceOf(ExpressCheckoutElement.PaymentMethod.GooglePay::class.java)
        assertThat(session.availableExpressCheckoutPaymentMethods[1])
            .isInstanceOf(ExpressCheckoutElement.PaymentMethod.Link::class.java)
    }

    @Test
    fun `session is restored from savedStateHandle after recreation`() = runTest {
        networkRule.defaultInit()
        val savedStateHandle = SavedStateHandle()
        val controller = createController(savedStateHandle)
        controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()

        // Simulate process death: persist the handle and build a new controller from the restored copy.
        val recreated = createController(savedStateHandle.simulateProcessDeath())

        assertThat(recreated.session.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
    }

    @Test
    fun `state is restored from savedStateHandle after recreation`() = runTest {
        networkRule.defaultInit()
        val savedStateHandle = SavedStateHandle()
        val controller = createController(savedStateHandle)
        controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()

        // Persisting and restoring the handle simulates the controller being rebuilt after process
        // death: the committed state is read back from the restored namespaced child.
        val recreated = createControllerSetup(
            savedStateHandle.simulateProcessDeath(),
            DEFAULT_INTEGRATION_NAME,
        )
        val state = recreated.stateHolder.state
        assertThat(state).isNotNull()
        assertThat(state!!.embeddedConfiguration.merchantDisplayName)
            .isEqualTo(expectedMerchantDisplayName)
    }

    @Test
    fun `destroy clears persisted state`() = runConfigureScenario {
        result.getOrThrow()
        // Pre-condition: configure committed a non-null state so the clear is observable.
        assertThat(committedState).isNotNull()

        controller.destroy()

        assertThat(committedState).isNull()
        assertThat(controller.session.value).isNull()
        assertThat(savedStateHandle.keys()).doesNotContain(DEFAULT_INTEGRATION_NAME)
        assertThat(createController(savedStateHandle.simulateProcessDeath()).session.value).isNull()
    }

    @Test
    fun `clearPaymentOption clears payment option state`() = runMutationScenario(
        paymentSelection = PaymentSelection.GooglePay,
        temporarySelection = "card",
        previousNewSelections = Bundle().apply {
            putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        },
    ) {
        controller.session.test {
            assertThat(awaitItem()?.paymentOptionDisplayData).isNotNull()

            controller.clearPaymentOption().getOrThrow()

            assertThat(requireNotNull(awaitItem()).paymentOptionDisplayData).isNull()
        }
        val clearedState = committedState()
        assertThat(clearedState.paymentSelection).isNull()
        assertThat(clearedState.temporarySelection).isNull()
        assertThat(clearedState.previousNewSelections.isEmpty).isTrue()
    }

    @Test
    fun `clearPaymentOption returns failure before the session is configured`() = runTest {
        val controller = createController()

        val result = controller.clearPaymentOption()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session before it is configured.")
    }

    @Test
    fun `clearPaymentOption returns failure and preserves selection when a payment flow is presented`() =
        runMutationScenario(paymentSelection = PaymentSelection.GooglePay, sheetIsOpen = true) {
            val result = controller.clearPaymentOption()

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            assertThat(result.exceptionOrNull()).hasMessageThat()
                .isEqualTo("Cannot mutate checkout session while a payment flow is presented.")
            // The rejected clear leaves the selection intact.
            assertThat(controller.session.value?.paymentOptionDisplayData).isNotNull()
        }

    @Test
    fun `clearPaymentOption waits for an in-flight mutation before clearing the selection`() =
        runMutationScenario(paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION) {
            val holdResponse = CountDownLatch(1)
            networkRule.checkoutUpdate(
                bodyPart("promotion_code", "10OFF"),
            ) { response ->
                holdResponse.await(10, TimeUnit.SECONDS)
                successResponseFactory().invoke(response)
            }
            val mutation = async { controller.applyPromotionCode("10OFF") }
            testScheduler.advanceUntilIdle()
            val clearPaymentOption = async { controller.clearPaymentOption() }
            testScheduler.advanceUntilIdle()

            assertThat(controller.session.value?.paymentOptionDisplayData).isNotNull()

            holdResponse.countDown()
            assertThat(mutation.await().isSuccess).isTrue()
            assertThat(clearPaymentOption.await().isSuccess).isTrue()
            assertThat(controller.session.value?.paymentOptionDisplayData).isNull()
        }

    @Test
    fun `default integration name is used as the payment element callback identifier`() = runTest {
        val controller = createController()

        assertThat(controller.paymentElementCallbackIdentifier).isEqualTo(DEFAULT_INTEGRATION_NAME)
    }

    @Test
    fun `custom integration name is used as the payment element callback identifier`() = runTest {
        val controller = createController(integrationName = "merchant_checkout")

        assertThat(controller.paymentElementCallbackIdentifier).isEqualTo("merchant_checkout")
    }

    @Test
    fun `integration name keys the controller into its own global callback references entry`() = runTest {
        val callbacks = PaymentElementCallbacks.Builder().build()
        PaymentElementCallbackReferences["merchant_checkout"] = callbacks

        val controller = createController(integrationName = "merchant_checkout")

        assertThat(PaymentElementCallbackReferences[controller.paymentElementCallbackIdentifier])
            .isSameInstanceAs(callbacks)
    }

    @Test
    fun `controllers with different integration names keep separate state on one saved state handle`() =
        runTest {
            networkRule.defaultInit()
            val savedStateHandle = SavedStateHandle()
            val first = createController(savedStateHandle, integrationName = "first")
            val second = createController(savedStateHandle, integrationName = "second")

            first.configure(DEFAULT_CLIENT_SECRET).getOrThrow()

            // Both controllers share the parent handle, but each persists under its own namespace, so
            // configuring the first leaves the second's state untouched.
            assertThat(first.session.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
            assertThat(second.session.value).isNull()
        }

    @Test
    fun `state does not leak across process death to a different integration name`() = runTest {
        networkRule.defaultInit()
        val savedStateHandle = SavedStateHandle()
        val controller = createController(savedStateHandle, integrationName = "first")
        controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()

        // After process death, a controller under a different name starts from empty state.
        val recreated = createController(savedStateHandle.simulateProcessDeath(), integrationName = "second")

        assertThat(recreated.session.value).isNull()
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
        val before = controller.session.value

        val result = controller.applyPromotionCode("INVALID")

        assertThat(result.isFailure).isTrue()
        assertThat(controller.session.value).isEqualTo(before)
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
    fun `updateCurrency sends updated_currency and updates session on success`() = runMutationScenario {
        networkRule.checkoutUpdate(
            bodyPart("updated_currency", "usd"),
            responseFactory = successResponseFactory { json ->
                json.put("total_summary", totalSummaryJson(due = 5099))
            },
        )

        val result = controller.updateCurrency("usd")

        result.getOrThrow()
        assertThat(controller.session.value?.totalSummary?.totalDueToday).isEqualTo(5099)
    }

    @Test
    fun `updateCurrency returns failure and preserves session on error`() = runMutationScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid currency"}}""")
        }
        val before = controller.session.value

        val result = controller.updateCurrency("invalid")

        assertThat(result.isFailure).isTrue()
        assertThat(controller.session.value).isEqualTo(before)
    }

    @Test
    fun `updateEmail stores email locally without a network call`() = runMutationScenario {
        // No checkoutUpdate is enqueued: email is client-only state and reloads from the existing
        // response, so NetworkRule will fail the test if a request is made.
        val result = controller.updateEmail("checkout@example.com")

        result.getOrThrow()
        assertThat(controller.session.value?.email).isEqualTo("checkout@example.com")
        assertThat(committedState().embeddedConfiguration.defaultBillingDetails?.email)
            .isEqualTo("checkout@example.com")
    }

    @Test
    fun `updateEmail clears email when session has no customer email`() = runMutationScenario(
        initModifier = { it.remove("customer_email") },
    ) {
        val result = controller.updateEmail(null)

        result.getOrThrow()
        assertThat(controller.session.value?.email).isNull()
    }

    @Test
    fun `updateEmail takes precedence over the session customer email`() = runMutationScenario {
        controller.updateEmail("local@example.com").getOrThrow()

        assertThat(controller.session.value?.email).isEqualTo("local@example.com")
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
                address = fullAddress,
            )

            result.getOrThrow()
            val state = committedState()
            assertThat(state.collectedDetails.shippingName).isEqualTo("John")
            assertThat(state.collectedDetails.shippingAddress).isEqualTo(fullAddress.build())
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
            val result = controller.updateShippingAddress(name = null, address = address)

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `updateShippingAddress stores address without a network call when automatic tax is disabled`() =
        runMutationScenario {
            // No checkoutUpdate is enqueued: with automatic tax off, the address is stored locally
            // and the payment element is reloaded from the existing response, firing no request.
            val result = controller.updateShippingAddress(name = "John", address = fullAddress)

            result.getOrThrow()
            val state = committedState()
            assertThat(state.collectedDetails.shippingName).isEqualTo("John")
            assertThat(state.collectedDetails.shippingAddress).isEqualTo(fullAddress.build())
        }

    @Test
    fun `updateShippingAddress does not store address on failure`() =
        runMutationScenario(initModifier = automaticTaxFor("shipping")) {
            networkRule.checkoutUpdate { response ->
                response.setResponseCode(400)
                response.setBody("""{"error": {"message": "Invalid address"}}""")
            }

            val result = controller.updateShippingAddress(name = "John", address = fullAddress)

            assertThat(result.isFailure).isTrue()
            val state = committedState()
            assertThat(state.collectedDetails.shippingName).isNull()
            assertThat(state.collectedDetails.shippingAddress).isNull()
        }

    @Test
    fun `updateShippingAddress does not send tax_region when automatic tax targets billing`() =
        runMutationScenario(initModifier = automaticTaxFor("billing")) {
            // Automatic tax targets billing, so a shipping address update stays local: no request.
            val result = controller.updateShippingAddress(name = "John", address = fullAddress)

            result.getOrThrow()
            val state = committedState()
            assertThat(state.collectedDetails.shippingName).isEqualTo("John")
            assertThat(state.collectedDetails.shippingAddress).isEqualTo(fullAddress.build())
        }

    @Test
    fun `commitShippingAddress stores local details and reloads payment element state`() =
        runMutationScenario {
            val response = committedState().checkoutSessionResponse
            val address = fullAddress.build()

            val result = controller.commitShippingAddress(
                name = "John",
                address = address,
            )

            result.getOrThrow()

            val state = committedState()
            assertThat(state.checkoutSessionResponse).isSameInstanceAs(response)
            assertThat(state.collectedDetails.shippingName).isEqualTo("John")
            assertThat(state.collectedDetails.shippingAddress).isEqualTo(address)
            assertThat(state.paymentMethodMetadata.shippingDetails?.name).isEqualTo("John")
            assertThat(state.paymentMethodMetadata.shippingDetails?.address).isEqualTo(
                address.asPaymentSheet()
            )
        }

    fun `runServerUpdate refreshes the session after serverUpdate completes`() = runMutationScenario {
        networkRule.checkoutInit(
            responseFactory = successResponseFactory { json ->
                json.put("total_summary", totalSummaryJson(due = 8000))
            },
        )

        val result = controller.runServerUpdate { Result.success(Unit) }

        result.getOrThrow()
        assertThat(controller.session.value?.totalSummary?.totalDueToday).isEqualTo(8000)
    }

    @Test
    fun `refresh commits a complete session status to the state holder`() = runMutationScenario {
        networkRule.checkoutInit(
            responseFactory = successResponseFactory { json -> json.put("status", "complete") },
        )

        val result = controller.runServerUpdate { Result.success(Unit) }

        assertThat(result.isSuccess).isTrue()
        assertThat(controller.session.value?.status)
            .isInstanceOf(CheckoutController.Session.Status.Complete::class.java)
    }

    @Test
    fun `runServerUpdate returns failure when serverUpdate throws`() = runMutationScenario {
        val before = controller.session.value

        val result = controller.runServerUpdate { throw IllegalStateException("Server error") }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("Server error")
        assertThat(controller.session.value).isEqualTo(before)
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
        val before = controller.session.value

        val result = controller.runServerUpdate { Result.success(Unit) }

        assertThat(result.isFailure).isTrue()
        assertThat(controller.session.value).isEqualTo(before)
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
    fun `mutation returns failure when a payment flow is presented`() = runMutationScenario(
        sheetIsOpen = true
    ) {
        val result = controller.applyPromotionCode("10OFF")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session while a payment flow is presented.")
    }

    @Test
    fun `runServerUpdate returns failure when a payment flow is presented`() = runMutationScenario(
        sheetIsOpen = true
    ) {
        val result = controller.runServerUpdate { Result.success(Unit) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session while a payment flow is presented.")
    }

    @Test
    fun `configure returns failure when a payment flow is presented`() = runMutationScenario(
        sheetIsOpen = true
    ) {
        val result = controller.configure(DEFAULT_CLIENT_SECRET)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()).hasMessageThat()
            .isEqualTo("Cannot mutate checkout session while a payment flow is presented.")
    }

    @Test
    fun `configure does not open a loading window when a payment flow is presented`() =
        runMutationScenario(sheetIsOpen = true, assertLoadingConsumed = true) {
            // The guard fast-fails before entering the coordinator, so isUpdating never flips true.
            assertThat(isUpdatingTurbine.awaitItem()).isFalse()

            val result = controller.configure(DEFAULT_CLIENT_SECRET)

            assertThat(result.isFailure).isTrue()
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
                bodyPart("updated_currency", "eur"),
                sessionId = "cs_test_after_promo",
                responseFactory = successResponseFactory { json -> json.put("session_id", "cs_test_after_promo") },
            )

            val results = listOf(
                async { controller.applyPromotionCode("10OFF") },
                async { controller.updateCurrency("eur") },
            ).awaitAll()

            assertThat(results[0].isSuccess).isTrue()
            assertThat(results[1].isSuccess).isTrue()
            assertThat(controller.session.value?.id).isEqualTo("cs_test_after_promo")
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

            assertThat(isUpdatingTurbine.awaitItem()).isFalse()

            val mutation = async { controller.applyPromotionCode("10OFF") }
            val configure = async { controller.configure(DEFAULT_CLIENT_SECRET) }
            testScheduler.advanceUntilIdle()

            assertThat(isUpdatingTurbine.awaitItem()).isTrue()
            // configure cannot complete while the mutation holds the mutex, proving it is serialized.
            assertThat(configure.isCompleted).isFalse()

            holdMutation.countDown()
            assertThat(mutation.await().isSuccess).isTrue()
            assertThat(configure.await().isSuccess).isTrue()

            // A single loading window spanned both operations, with no flicker to false in between.
            assertThat(isUpdatingTurbine.awaitItem()).isFalse()
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
            val before = controller.session.value

            // Fast-fail returns before entering the coordinator, so isUpdating never flips true.
            assertThat(isUpdatingTurbine.awaitItem()).isFalse()

            val result = controller.updateShippingAddress(
                name = null,
                address = Address().country("DE"),
            )

            assertThat(result.isFailure).isTrue()
            val exception = result.exceptionOrNull()
            assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(exception).hasMessageThat().isEqualTo(
                "Country code 'DE' is not in allowedShippingCountries"
            )
            assertThat(controller.session.value).isEqualTo(before)
        }

    @Test
    fun `updateShippingAddress succeeds when allowedShippingCountries is null`() =
        runMutationScenario {
            // No allowlist set, so any country passes.
            val result = controller.updateShippingAddress(
                name = null,
                address = Address().country("DE"),
            )

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `updateShippingAddress fails for all countries when allowedShippingCountries is empty`() =
        runMutationScenario(initModifier = allowedShippingCountries(emptyList())) {
            val before = controller.session.value

            val result = controller.updateShippingAddress(
                name = null,
                address = Address().country("US"),
            )

            assertThat(result.isFailure).isTrue()
            val exception = result.exceptionOrNull()
            assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(exception).hasMessageThat().isEqualTo(
                "Country code 'US' is not in allowedShippingCountries"
            )
            assertThat(controller.session.value).isEqualTo(before)
        }

    @Test
    fun `updateShippingAddress with missing country throws IllegalArgumentException before allowlist check`() =
        runMutationScenario(initModifier = allowedShippingCountries(listOf("US"))) {
            // Address.build() requires a country and throws synchronously, before the allowlist is
            // ever consulted, so the call is wrapped to capture the thrown exception.
            val result = runCatching {
                controller.updateShippingAddress(name = null, address = Address())
            }

            assertThat(result.isFailure).isTrue()
            val exception = result.exceptionOrNull()
            assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(exception).hasMessageThat().isEqualTo("Country is required.")
        }

    // endregion

    private fun NetworkRule.defaultInit() {
        checkoutInit(responseFactory = ::successResponse)
    }

    // The base fixture omits customer_email. Inject one for standard success paths; a test can
    // remove it in the JSON modifier when exercising an absent session email.
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

    @Suppress("RestrictedApi")
    private fun parentHandleWithState(state: CheckoutControllerState): SavedStateHandle {
        val childHandle = SavedStateHandle(
            mapOf(CheckoutControllerStateHolder.STATE_KEY to state)
        )
        return SavedStateHandle(
            mapOf(DEFAULT_INTEGRATION_NAME to childHandle.savedStateProvider().saveState())
        )
    }

    private fun createController(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        integrationName: String = DEFAULT_INTEGRATION_NAME,
    ): CheckoutController {
        return destroyControllerRule.track(
            CheckoutController.Builder(
                application = applicationContext,
                savedStateHandle = savedStateHandle,
            ).integrationName(integrationName).build()
        )
    }

    private fun createSession(
        availableExpressButtonTypes: List<ExpressButtonType>,
    ): CheckoutController.Session {
        return CheckoutController.Session(
            id = DEFAULT_CHECKOUT_SESSION_ID,
            status = CheckoutController.Session.Status.Open(),
            liveMode = false,
            currency = "usd",
            email = null,
            tax = CheckoutController.Session.Tax(CheckoutController.Session.Tax.Status.Ready),
            totalSummary = null,
            lineItems = emptyList(),
            shippingOptions = emptyList(),
            paymentOptionDisplayData = null,
            currencySelectorOptions = null,
            availableExpressButtonTypes = availableExpressButtonTypes,
        )
    }

    private fun createControllerSetup(
        savedStateHandle: SavedStateHandle,
        integrationName: String,
    ): ControllerSetup {
        val controllerSavedState = CheckoutControllerSavedState(
            parentHandle = savedStateHandle,
            integrationName = integrationName,
        )
        val controller = destroyControllerRule.track(
            DaggerCheckoutControllerComponent.factory().create(
                application = applicationContext,
                paymentElementCallbackIdentifier = integrationName,
                resultCallback = CheckoutController.ResultCallback {},
                rowSelectionBehavior = PaymentElement.RowSelectionBehavior.default(),
                checkoutControllerSavedState = controllerSavedState,
            ).checkoutController
        )
        return ControllerSetup(
            controller = controller,
            stateHolder = CheckoutControllerStateFactory.createStateHolder(controllerSavedState.handle),
            sheetStateHolder = SheetStateHolder(controllerSavedState.handle),
        )
    }

    private class ControllerSetup(
        val controller: CheckoutController,
        val stateHolder: CheckoutControllerStateHolder,
        val sheetStateHolder: SheetStateHolder,
    )

    private fun runConfigureScenario(
        clientSecret: String = DEFAULT_CLIENT_SECRET,
        configuration: CheckoutController.Configuration = CheckoutController.Configuration(),
        networkSetup: () -> Unit = { networkRule.defaultInit() },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        networkSetup()
        val savedStateHandle = SavedStateHandle()
        val setup = createControllerSetup(savedStateHandle, DEFAULT_INTEGRATION_NAME)
        val result = setup.controller.configure(clientSecret, configuration)
        block(Scenario(setup.controller, result, savedStateHandle, setup.stateHolder))
    }

    private class Scenario(
        val controller: CheckoutController,
        val result: Result<Unit>,
        val savedStateHandle: SavedStateHandle,
        private val stateHolder: CheckoutControllerStateHolder,
    ) {
        val committedState: CheckoutControllerState?
            get() = stateHolder.state
    }

    // Configures a controller from a fresh init, seeds the requested scenario state, then hands it
    // to [block] alongside an isUpdating Turbine.
    //
    // Set [assertLoadingConsumed] for tests that verify loading behavior: the block must consume
    // every isUpdating emission and this asserts none are left over. Tests that don't care about
    // loading leave it false, and any unconsumed emissions are ignored.
    private fun runMutationScenario(
        initModifier: (JSONObject) -> Unit = {},
        paymentSelection: PaymentSelection? = null,
        temporarySelection: String? = null,
        previousNewSelections: Bundle = Bundle(),
        sheetIsOpen: Boolean = false,
        assertLoadingConsumed: Boolean = false,
        block: suspend MutationScenario.() -> Unit,
    ) = runTest {
        networkRule.checkoutInit(
            responseFactory = successResponseFactory(
                jsonModifier = initModifier,
            )
        )
        val savedStateHandle = SavedStateHandle()
        val setup = createControllerSetup(savedStateHandle, DEFAULT_INTEGRATION_NAME)
        val controller = setup.controller
        controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
        paymentSelection?.let(setup.stateHolder::setSelection)
        temporarySelection?.let(setup.stateHolder::setTemporarySelection)
        if (!previousNewSelections.isEmpty) {
            setup.stateHolder.setPreviousNewSelections(previousNewSelections)
        }
        setup.sheetStateHolder.sheetIsOpen = sheetIsOpen

        turbineScope {
            val isUpdatingTurbine = controller.isUpdating.testIn(backgroundScope)
            block(
                MutationScenario(
                    controller = controller,
                    stateHolder = setup.stateHolder,
                    testScope = this@runTest,
                    isUpdatingTurbine = isUpdatingTurbine,
                )
            )
            if (assertLoadingConsumed) {
                isUpdatingTurbine.ensureAllEventsConsumed()
            } else {
                isUpdatingTurbine.cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private class MutationScenario(
        val controller: CheckoutController,
        private val stateHolder: CheckoutControllerStateHolder,
        private val testScope: TestScope,
        val isUpdatingTurbine: ReceiveTurbine<Boolean>,
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
        fun committedState(): CheckoutControllerState = requireNotNull(stateHolder.state)
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val DEFAULT_INTEGRATION_NAME = "stripe_checkout"
    }
}
