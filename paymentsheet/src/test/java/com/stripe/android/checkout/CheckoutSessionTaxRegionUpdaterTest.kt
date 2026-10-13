package com.stripe.android.checkout

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.checkout.analytics.CheckoutAnalyticsOperation
import com.stripe.android.checkout.analytics.CheckoutEventReporterFactory
import com.stripe.android.checkout.analytics.CheckoutInvocationSource
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutSessionTaxRegionUpdaterTest {

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `requiresUpdate returns true when automatic tax address source matches`() = runScenario {
        assertThat(
            updater.requiresUpdate(
                checkoutSessionResponse = checkoutSessionResponse,
                addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            )
        ).isTrue()
    }

    @Test
    fun `requiresUpdate returns false when automatic tax address source does not match`() = runScenario {
        assertThat(
            updater.requiresUpdate(
                checkoutSessionResponse = checkoutSessionResponse,
                addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            )
        ).isFalse()
    }

    @Test
    fun `updateServerStateIfNeeded updates tax region when automatic tax address source matches`() = runScenario {
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", "US"),
            bodyPart("tax_region[line1]", "510 Townsend St"),
            bodyPart("tax_region[city]", "San Francisco"),
            bodyPart("tax_region[state]", "CA"),
            bodyPart("tax_region[postal_code]", "94103"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
                .setHeader("request-id", "req_fresh_tax_region_update")
        }

        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
            invocationSource = CheckoutInvocationSource.PaymentElement,
        )

        val updatedResponse = result.getOrThrow()
        assertThat(updatedResponse.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        assertThat(updatedResponse.requestId).isEqualTo("req_fresh_tax_region_update")
        assertAddressEventPair(
            operation = CheckoutAnalyticsOperation.UpdateBillingAddress,
            didRequestTaxUpdate = true,
            requestId = "req_fresh_tax_region_update",
        )
    }

    @Test
    fun `updateServerStateIfNeeded returns original response when automatic tax is disabled`() = runScenario(
        automaticTaxEnabled = false,
        requestId = "req_previous_request",
    ) {
        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
            invocationSource = CheckoutInvocationSource.PaymentElement,
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
        assertAddressEventPair(
            operation = CheckoutAnalyticsOperation.UpdateBillingAddress,
            didRequestTaxUpdate = false,
        )
        assertThat(reportedEvents().last()).doesNotContainKey("request_id")
    }

    @Test
    fun `updateServerStateIfNeeded returns original response when address source does not match`() = runScenario(
        requestId = "req_previous_request",
    ) {
        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            address = ADDRESS,
            invocationSource = CheckoutInvocationSource.ExpressCheckoutElement,
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
        assertAddressEventPair(
            operation = CheckoutAnalyticsOperation.UpdateShippingAddress,
            didRequestTaxUpdate = false,
            invocationSource = CheckoutInvocationSource.ExpressCheckoutElement,
        )
        assertThat(reportedEvents().last()).doesNotContainKey("request_id")
    }

    @Test
    fun `updateServerStateIfNeeded returns failure when tax region update fails`() = runScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
        }

        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
            invocationSource = CheckoutInvocationSource.PaymentElement,
        )

        assertThat(result.exceptionOrNull()?.message).contains("Invalid tax region")
        assertAddressEventPair(
            operation = CheckoutAnalyticsOperation.UpdateBillingAddress,
            didRequestTaxUpdate = true,
            terminalPhase = "failed",
        )
    }

    @Test
    fun `updateServerStateIfNeeded does not report when outer operation owns reporting`() = runScenario {
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", "US"),
            bodyPart("tax_region[line1]", "510 Townsend St"),
            bodyPart("tax_region[city]", "San Francisco"),
            bodyPart("tax_region[state]", "CA"),
            bodyPart("tax_region[postal_code]", "94103"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
            invocationSource = null,
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(analyticsRequestExecutor.getExecutedRequests()).isEmpty()
    }

    @Test
    fun `updateServerStateIfNeeded omits invocation source when it is unspecified`() = runScenario(
        automaticTaxEnabled = false,
    ) {
        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
            invocationSource = CheckoutInvocationSource.Unspecified,
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
        assertAddressEventPair(
            operation = CheckoutAnalyticsOperation.UpdateBillingAddress,
            didRequestTaxUpdate = false,
            invocationSource = CheckoutInvocationSource.Unspecified,
        )
    }

    private fun runScenario(
        automaticTaxEnabled: Boolean = true,
        requestId: String? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val checkoutSessionRepository = CheckoutSessionRepository(
            stripeNetworkClient = DefaultStripeNetworkClient(),
            analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                publishableKey = "pk_test_123",
            ),
            apiRequestOptionsProvider = {
                ApiRequest.Options(
                    apiKey = DEFAULT_API_CONFIG.publishableKey,
                    stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
                )
            },
        )

        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val updater = CheckoutSessionTaxRegionUpdater(
            checkoutSessionRepository = checkoutSessionRepository,
            eventReporter = CheckoutEventReporterFactory.create(analyticsRequestExecutor),
        )

        val scenario = Scenario(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = automaticTaxEnabled,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                requestId = requestId,
            ),
            checkoutSessionRepository = checkoutSessionRepository,
            updater = updater,
            analyticsRequestExecutor = analyticsRequestExecutor,
        )

        scenario.block()
    }

    private data class Scenario(
        val checkoutSessionResponse: CheckoutSessionResponse,
        val checkoutSessionRepository: CheckoutSessionRepository,
        val updater: CheckoutSessionTaxRegionUpdater,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
    )

    private fun Scenario.assertAddressEventPair(
        operation: CheckoutAnalyticsOperation,
        didRequestTaxUpdate: Boolean,
        invocationSource: CheckoutInvocationSource? = CheckoutInvocationSource.PaymentElement,
        terminalPhase: String = "succeeded",
        requestId: String? = null,
    ) {
        val events = reportedEvents()
        val eventPrefix = "checkout_elements.${operation.value}"

        assertThat(events).hasSize(2)
        assertThat(events.map { it["event"] }).containsExactly(
            "$eventPrefix.started",
            "$eventPrefix.$terminalPhase",
        ).inOrder()
        events.forEach { params ->
            assertThat(params).containsEntry("did_request_tax_update", didRequestTaxUpdate)
            val source = invocationSource?.value
            if (source == null) {
                assertThat(params).doesNotContainKey("invocation_source")
            } else {
                assertThat(params).containsEntry("invocation_source", source)
            }
        }
        if (requestId != null) {
            assertThat(events.first()).doesNotContainKey("request_id")
            assertThat(events.last()).containsEntry("request_id", requestId)
        }
    }

    private fun Scenario.reportedEvents() = analyticsRequestExecutor.getExecutedRequests().map { it.params }

    private companion object {
        val ADDRESS = Address.State(
            city = "San Francisco",
            country = "US",
            line1 = "510 Townsend St",
            line2 = null,
            postalCode = "94103",
            state = "CA",
        )
    }
}
