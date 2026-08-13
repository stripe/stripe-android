package com.stripe.android.checkout

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.ElementsSessionClientParams
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

    private val checkoutSessionRepository = CheckoutSessionRepository(
        clientParams = ElementsSessionClientParams(
            mobileAppId = "com.stripe.android.paymentsheet.test",
            mobileSessionIdProvider = { "test_session" },
        ),
        stripeNetworkClient = DefaultStripeNetworkClient(),
        analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
        paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = "pk_test_123",
        ),
        publishableKeyProvider = { "pk_test_123" },
        stripeAccountIdProvider = { null },
    )

    private val updater = CheckoutSessionTaxRegionUpdater(checkoutSessionRepository)

    @Test
    fun `updateIfNeeded updates tax region when automatic tax address source matches`() = runScenario {
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", "US"),
            bodyPart("tax_region[line1]", "510 Townsend St"),
            bodyPart("tax_region[city]", "San Francisco"),
            bodyPart("tax_region[state]", "CA"),
            bodyPart("tax_region[postal_code]", "94103"),
            bodyPart("elements_session_client[is_aggregation_expected]", "true"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = updater.updateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
        )

        assertThat(result.getOrThrow().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
    }

    @Test
    fun `updateIfNeeded returns original response when automatic tax is disabled`() = runScenario(
        automaticTaxEnabled = false,
    ) {
        val result = updater.updateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
    }

    @Test
    fun `updateIfNeeded returns original response when address source does not match`() = runScenario {
        val result = updater.updateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            address = ADDRESS,
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(checkoutSessionResponse)
    }

    @Test
    fun `updateIfNeeded returns failure when tax region update fails`() = runScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
        }

        val result = updater.updateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            address = ADDRESS,
        )

        assertThat(result.exceptionOrNull()?.message).contains("Invalid tax region")
    }

    private fun runScenario(
        automaticTaxEnabled: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val scenario = Scenario(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = automaticTaxEnabled,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
        )

        scenario.block()
    }

    private data class Scenario(
        val checkoutSessionResponse: CheckoutSessionResponse,
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
