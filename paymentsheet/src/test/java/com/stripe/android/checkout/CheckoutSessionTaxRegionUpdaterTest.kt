package com.stripe.android.checkout

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Address
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
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeErrorReporter
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
    fun `updateServerStateIfNeeded reports and skips the update when a required address is missing`() = runScenario {
        val result = updater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = null,
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
        assertThat(errorReporter.awaitCall()).isEqualTo(
            FakeErrorReporter.Call(
                errorEvent = ErrorReporter.UnexpectedErrorEvent.CHECKOUT_TAX_REGION_UPDATE_MISSING_ADDRESS,
                stripeException = null,
                additionalNonPiiParams = mapOf("address_source" to "BILLING"),
            )
        )
    }

    @Test
    fun `updateServerStateIfNeeded does not report a missing address when address source does not match`() =
        runScenario {
            val result = updater.updateServerStateIfNeeded(
                checkoutSessionResponse = checkoutSessionResponse,
                addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                address = null,
            )

            assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
        }

    @Test
    fun `updateServerStateIfNeeded does not report a missing address when automatic tax is disabled`() =
        runScenario(automaticTaxEnabled = false) {
            val result = updater.updateServerStateIfNeeded(
                checkoutSessionResponse = checkoutSessionResponse,
                addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
                address = null,
            )

            assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
        }

    private fun runScenario(
        automaticTaxEnabled: Boolean = true,
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

        val errorReporter = FakeErrorReporter()
        val updater = CheckoutSessionTaxRegionUpdater(
            checkoutSessionRepository = checkoutSessionRepository,
            errorReporter = errorReporter,
        )

        val scenario = Scenario(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = automaticTaxEnabled,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
            checkoutSessionRepository = checkoutSessionRepository,
            updater = updater,
            errorReporter = errorReporter,
        )

        scenario.block()

        errorReporter.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val checkoutSessionResponse: CheckoutSessionResponse,
        val checkoutSessionRepository: CheckoutSessionRepository,
        val updater: CheckoutSessionTaxRegionUpdater,
        val errorReporter: FakeErrorReporter,
    )

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
