package com.stripe.android.checkout

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutElementsController.Address
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

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

        assertThat(result.result.getOrThrow().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        assertThat(result.requestAttempted).isTrue()
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

        assertThat(result.result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
        assertThat(result.requestAttempted).isFalse()
    }

    @Test
    fun `updateServerStateIfNeeded returns original response when address source does not match`() = runScenario {
        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            address = ADDRESS,
        )

        assertThat(result.result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
        assertThat(result.requestAttempted).isFalse()
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

        assertThat(result.result.exceptionOrNull()?.message).contains("Invalid tax region")
        assertThat(result.requestAttempted).isTrue()
    }

    @Test
    fun `canceling the request preserves the attempted fact and cancellation`() {
        val networkClient = FakeCancelingNetworkClient()
        runScenario(stripeNetworkClient = networkClient) {
            val outcomes = Turbine<TaxRegionUpdateOutcome>()
            val update = testScope.launch {
                outcomes.add(
                    updater.updateServerStateIfNeeded(
                        checkoutSessionResponse = checkoutSessionResponse,
                        addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                        address = ADDRESS,
                    )
                )
            }
            networkClient.requests.awaitItem()

            update.cancelAndJoin()

            val outcome = outcomes.awaitItem()
            assertThat(outcome.requestAttempted).isTrue()
            assertThat(outcome.result.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
            assertThat(runCatching { outcome.resultOrThrowCancellation() }.exceptionOrNull())
                .isSameInstanceAs(outcome.result.exceptionOrNull())
            networkClient.requests.ensureAllEventsConsumed()
            outcomes.ensureAllEventsConsumed()
        }
    }

    private fun runScenario(
        automaticTaxEnabled: Boolean = true,
        stripeNetworkClient: StripeNetworkClient = DefaultStripeNetworkClient(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val checkoutSessionRepository = CheckoutSessionRepository(
            stripeRepository = CheckoutStripeRepositoryFactory.create(),
            elementsSessionClientParams = CheckoutStripeRepositoryFactory.clientParams(),
            fraudDetectionDataRepository = FakeFraudDetectionDataRepository.create(),
            stripeNetworkClient = stripeNetworkClient,
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

        val scenario = Scenario(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = automaticTaxEnabled,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
            checkoutSessionRepository = checkoutSessionRepository,
            updater = updater,
            testScope = this,
        )

        scenario.block()
    }

    private data class Scenario(
        val checkoutSessionResponse: CheckoutSessionResponse,
        val checkoutSessionRepository: CheckoutSessionRepository,
        val updater: CheckoutSessionTaxRegionUpdater,
        val testScope: TestScope,
    )

    private class FakeCancelingNetworkClient : StripeNetworkClient {
        val requests = Turbine<StripeRequest>()

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            requests.add(request)
            awaitCancellation()
        }

        override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> {
            error("Not expected")
        }
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
