package com.stripe.android.checkout

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutElementsController.Address
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent
import com.stripe.android.checkout.analytics.MeasuredOperation
import com.stripe.android.checkout.analytics.updateServerStateIfNeeded
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
import com.stripe.android.paymentsheet.repositories.CheckoutStripeRepositoryFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeFraudDetectionDataRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.TimeSource

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
        }

        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
        )

        assertThat(result.getOrThrow().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
    }

    @Test
    fun `updateServerStateIfNeeded returns original response when automatic tax is disabled`() = runScenario(
        automaticTaxEnabled = false,
    ) {
        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
    }

    @Test
    fun `updateServerStateIfNeeded returns original response when address source does not match`() = runScenario {
        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            address = ADDRESS,
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
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
        )

        assertThat(result.exceptionOrNull()?.message).contains("Invalid tax region")
    }

    @Test
    fun `operation extension marks the tax update as requested when an update is required`() = runScenario {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }
        val operation = measuredOperation()

        val result = updater.updateServerStateIfNeeded(
            operation = operation,
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
        )

        assertThat(result.getOrThrow().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        operation.succeeded(operation.response)
        assertThat(reportedEvents.single().params).containsEntry("did_request_tax_update", true)
    }

    @Test
    fun `operation extension marks the tax update as requested when the request fails`() = runScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
        }
        val operation = measuredOperation()

        val result = updater.updateServerStateIfNeeded(
            operation = operation,
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
        )

        assertThat(result.isFailure).isTrue()
        operation.failed(checkoutSessionResponse, result.exceptionOrNull()!!)
        assertThat(reportedEvents.single().params).containsEntry("did_request_tax_update", true)
    }

    @Test
    fun `operation extension leaves the tax update unrequested when the address source does not match`() = runScenario {
        val operation = measuredOperation()

        val result = updater.updateServerStateIfNeeded(
            operation = operation,
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            address = ADDRESS,
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
        operation.succeeded(operation.response)
        assertThat(reportedEvents.single().params).containsEntry("did_request_tax_update", false)
    }

    @Test
    fun `operation extension leaves the tax update unrequested when automatic tax is disabled`() = runScenario(
        automaticTaxEnabled = false,
    ) {
        val operation = measuredOperation()

        val result = updater.updateServerStateIfNeeded(
            operation = operation,
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
        operation.succeeded(operation.response)
        assertThat(reportedEvents.single().params).containsEntry("did_request_tax_update", false)
    }

    private fun runScenario(
        automaticTaxEnabled: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val checkoutSessionRepository = CheckoutSessionRepository(
            stripeRepository = CheckoutStripeRepositoryFactory.create(),
            elementsSessionClientParams = CheckoutStripeRepositoryFactory.clientParams(),
            fraudDetectionDataRepository = FakeFraudDetectionDataRepository.create(),
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

        val updater = CheckoutSessionTaxRegionUpdater(checkoutSessionRepository)

        val reportedEvents = mutableListOf<CheckoutAnalyticsEvent>()
        val scenario = Scenario(
            reportedEvents = reportedEvents,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = automaticTaxEnabled,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
            checkoutSessionRepository = checkoutSessionRepository,
            updater = updater,
        )

        scenario.block()
    }

    private data class Scenario(
        val reportedEvents: MutableList<CheckoutAnalyticsEvent>,
        val checkoutSessionResponse: CheckoutSessionResponse,
        val checkoutSessionRepository: CheckoutSessionRepository,
        val updater: CheckoutSessionTaxRegionUpdater,
    ) {
        fun measuredOperation() = MeasuredOperation(
            operation = CheckoutAnalyticsEvent.Operation.UpdateBillingAddress,
            startedParams = emptyMap(),
            response = checkoutSessionResponse,
            timeSource = TimeSource.Monotonic,
            report = { reportedEvents += it },
        )
    }

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
