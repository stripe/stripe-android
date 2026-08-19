package com.stripe.android.paymentelement.embedded.sheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.PaymentSelection
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
internal class SheetTaxRegionUpdaterTest {

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `update sends the selection billing address and returns the updated response`() = runScenario {
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", "US"),
            bodyPart("tax_region[line1]", "510 Townsend St"),
            bodyPart("tax_region[line2]", "Suite 100"),
            bodyPart("tax_region[city]", "San Francisco"),
            bodyPart("tax_region[state]", "CA"),
            bodyPart("tax_region[postal_code]", "94103"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = updater.update(selectionWithAddress(ADDRESS))

        assertThat(result.getOrThrow()?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
    }

    @Test
    fun `update returns null when selection is null`() = runScenario {
        val result = updater.update(selection = null)

        assertThat(result.getOrThrow()).isNull()
    }

    @Test
    fun `update returns null when selection has no billing address`() = runScenario {
        val selection = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                billingDetails = PaymentMethod.BillingDetails(address = null),
            ),
        )

        val result = updater.update(selection)

        assertThat(result.getOrThrow()).isNull()
    }

    @Test
    fun `update returns null when billing address has no country`() = runScenario {
        val result = updater.update(selectionWithAddress(ADDRESS.copy(country = null)))

        assertThat(result.getOrThrow()).isNull()
    }

    @Test
    fun `update returns failure when the tax region update fails`() = runScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
        }

        val result = updater.update(selectionWithAddress(ADDRESS))

        assertThat(result.exceptionOrNull()?.message).contains("Invalid tax region")
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val checkoutSessionRepository = CheckoutSessionRepository(
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
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )
        val updater = SheetTaxRegionUpdater(
            checkoutSessionResponse = checkoutSessionResponse,
            taxRegionUpdater = CheckoutSessionTaxRegionUpdater(checkoutSessionRepository),
        )

        Scenario(updater = updater).block()
    }

    private data class Scenario(
        val updater: SheetTaxRegionUpdater,
    )

    private companion object {
        val ADDRESS = Address(
            city = "San Francisco",
            country = "US",
            line1 = "510 Townsend St",
            line2 = "Suite 100",
            postalCode = "94103",
            state = "CA",
        )

        fun selectionWithAddress(address: Address): PaymentSelection {
            return PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                    billingDetails = PaymentMethod.BillingDetails(address = address),
                ),
            )
        }
    }
}
