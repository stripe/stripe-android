package com.stripe.android.paymentelement.embedded.sheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
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
    fun `prepareUpdate returns null when checkout session does not collect tax from billing address`() = runScenario(
        paymentMethodMetadata = paymentMethodMetadata(
            checkoutSessionResponse(
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            )
        )
    ) {
        val update = updater.prepareUpdate(paymentMethodMetadata, selectionWithAddress(ADDRESS))

        assertThat(update).isNull()
    }

    @Test
    fun `prepareUpdate returns null for non-checkout session integration`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    ) {
        val update = updater.prepareUpdate(paymentMethodMetadata, selectionWithAddress(ADDRESS))

        assertThat(update).isNull()
    }

    @Test
    fun `prepareUpdate returns null when automatic tax is disabled`() = runScenario(
        paymentMethodMetadata = paymentMethodMetadata(
            checkoutSessionResponse(
                automaticTaxEnabled = false,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            )
        )
    ) {
        val update = updater.prepareUpdate(paymentMethodMetadata, selectionWithAddress(ADDRESS))

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
            updater.prepareUpdate(paymentMethodMetadata, selectionWithAddress(ADDRESS))
        ).invoke()

        assertThat(result.getOrThrow().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
    }

    @Test
    fun `prepareUpdate returns null when selection is null`() = runScenario {
        val update = updater.prepareUpdate(paymentMethodMetadata, selection = null)

        assertThat(update).isNull()
    }

    @Test
    fun `prepareUpdate returns null when selection has no billing address`() = runScenario {
        val selection = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                billingDetails = PaymentMethod.BillingDetails(address = null),
            ),
        )

        val update = updater.prepareUpdate(paymentMethodMetadata, selection)

        assertThat(update).isNull()
    }

    @Test
    fun `prepareUpdate returns null when billing address has no country`() = runScenario {
        val update = updater.prepareUpdate(
            paymentMethodMetadata,
            selectionWithAddress(ADDRESS.copy(country = null)),
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
            updater.prepareUpdate(paymentMethodMetadata, selectionWithAddress(ADDRESS))
        ).invoke()

        assertThat(result.exceptionOrNull()?.message).contains("Invalid tax region")
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runScenario(
        paymentMethodMetadata = paymentMethodMetadata(
            checkoutSessionResponse(
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            )
        ),
        block = block,
    )

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val updater = SheetTaxRegionUpdater(
            taxRegionUpdater = checkoutSessionTaxRegionUpdater(),
        )

        Scenario(
            updater = updater,
            paymentMethodMetadata = paymentMethodMetadata,
        ).block()
    }

    private fun checkoutSessionTaxRegionUpdater(): CheckoutSessionTaxRegionUpdater {
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

        return CheckoutSessionTaxRegionUpdater(checkoutSessionRepository)
    }

    private fun paymentMethodMetadata(
        checkoutSessionResponse: CheckoutSessionResponse,
    ) = PaymentMethodMetadataFactory.create(
        integrationMetadata = IntegrationMetadata.CheckoutSession(
            id = checkoutSessionResponse.id,
            instancesKey = "test_instances_key",
            checkoutSessionResponse = checkoutSessionResponse,
        )
    )

    private fun checkoutSessionResponse(
        automaticTaxEnabled: Boolean,
        taxAddressSource: CheckoutSessionResponse.TaxAddressSource,
    ) = CheckoutSessionResponseFactory.create(
        automaticTaxEnabled = automaticTaxEnabled,
        taxAddressSource = taxAddressSource,
    )

    private data class Scenario(
        val updater: SheetTaxRegionUpdater,
        val paymentMethodMetadata: PaymentMethodMetadata,
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
