package com.stripe.android.paymentelement.embedded.sheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
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
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.utils.FakeFraudDetectionDataRepository
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
    fun `prepareUpdate returns null when checkout session does not collect tax from billing address`() = runScenario(
        response = checkoutSessionResponse(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        ),
    ) {
        val update = updater.prepareUpdate(
            checkoutSessionResponse = response,
            selection = selectionWithAddress(ADDRESS),
        )

        assertThat(update).isNull()
    }

    @Test
    fun `prepareUpdate returns null when checkout session response is null`() = runScenario(
        response = null,
    ) {
        val update = updater.prepareUpdate(
            checkoutSessionResponse = response,
            selection = selectionWithAddress(ADDRESS),
        )

        assertThat(update).isNull()
    }

    @Test
    fun `prepareUpdate returns null when automatic tax is disabled`() = runScenario(
        response = checkoutSessionResponse(
            automaticTaxEnabled = false,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        ),
    ) {
        val update = updater.prepareUpdate(
            checkoutSessionResponse = response,
            selection = selectionWithAddress(ADDRESS),
        )

        assertThat(update).isNull()
    }

    @Test
    fun `prepared update sends the selection billing address and returns the updated response`() = runScenario {
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

        val result = requireNotNull(
            updater.prepareUpdate(
                checkoutSessionResponse = response,
                selection = selectionWithAddress(ADDRESS),
            )
        ).invoke()

        assertThat(result.getOrThrow().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
    }

    @Test
    fun `prepareUpdate returns null when selection is null`() = runScenario {
        val update = updater.prepareUpdate(
            checkoutSessionResponse = response,
            selection = null,
        )

        assertThat(update).isNull()
    }

    @Test
    fun `prepareUpdate returns null when selection has no billing address`() = runScenario {
        val selection = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                billingDetails = PaymentMethod.BillingDetails(address = null),
            ),
        )

        val update = updater.prepareUpdate(
            checkoutSessionResponse = response,
            selection = selection,
        )

        assertThat(update).isNull()
    }

    @Test
    fun `prepareUpdate returns null when billing address has no country`() = runScenario {
        val update = updater.prepareUpdate(
            checkoutSessionResponse = response,
            selection = selectionWithAddress(ADDRESS.copy(country = null)),
        )

        assertThat(update).isNull()
    }

    @Test
    fun `prepared update returns failure when the tax region update fails`() = runScenario {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
        }

        val result = requireNotNull(
            updater.prepareUpdate(
                checkoutSessionResponse = response,
                selection = selectionWithAddress(ADDRESS),
            )
        ).invoke()

        assertThat(result.exceptionOrNull()?.message).contains("Invalid tax region")
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runScenario(
        response = checkoutSessionResponse(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        ),
        block = block,
    )

    private fun runScenario(
        response: CheckoutSessionResponse?,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val updater = SheetTaxRegionUpdater(
            taxRegionUpdater = checkoutSessionTaxRegionUpdater(),
        )

        Scenario(
            updater = updater,
            response = response,
        ).block()
    }

    private fun checkoutSessionTaxRegionUpdater(): CheckoutSessionTaxRegionUpdater {
        val checkoutSessionRepository = CheckoutSessionRepository(
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
                    betas = emptySet(),
                )
            },
        )

        return CheckoutSessionTaxRegionUpdater(checkoutSessionRepository)
    }

    private fun checkoutSessionResponse(
        automaticTaxEnabled: Boolean,
        taxAddressSource: CheckoutSessionResponse.TaxAddressSource,
    ) = CheckoutSessionResponseFactory.create(
        automaticTaxEnabled = automaticTaxEnabled,
        taxAddressSource = taxAddressSource,
    )

    private data class Scenario(
        val updater: SheetTaxRegionUpdater,
        val response: CheckoutSessionResponse?,
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
