package com.stripe.android.paymentsheet.state

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class DefaultRetrieveCustomerEmailTest {

    @Test
    fun `default billing email takes priority and skips API call`() = runScenario(
        configuration = CONFIG_WITH_DEFAULT_EMAIL,
        customerMetadata = CUSTOMER_SESSION_METADATA,
    ) {
        assertThat(result).isEqualTo("default@example.com")
    }

    @Test
    fun `customer session calls repository with session credentials when no default email`() = runScenario(
        configuration = CONFIG_WITHOUT_EMAIL,
        customerMetadata = CUSTOMER_SESSION_METADATA,
    ) {
        assertThat(result).isNull()
        val call = customerRepository.retrieveCalls.awaitItem()
        assertThat(call.customerId).isEqualTo("cus_1")
        assertThat(call.ephemeralKeySecret).isEqualTo("ek_test_session")
    }

    @Test
    fun `legacy ephemeral key calls repository with legacy credentials when no default email`() = runScenario(
        configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
        customerMetadata = LEGACY_EK_METADATA,
    ) {
        assertThat(result).isNull()
        val call = customerRepository.retrieveCalls.awaitItem()
        assertThat(call.customerId).isEqualTo("cus_123")
        assertThat(call.ephemeralKeySecret).isEqualTo(PaymentSheetFixtures.DEFAULT_EPHEMERAL_KEY)
    }

    @Test
    fun `checkout session returns default billing email without calling repository`() = runScenario(
        configuration = CONFIG_WITH_DEFAULT_EMAIL,
        customerMetadata = CHECKOUT_SESSION_METADATA,
    ) {
        assertThat(result).isEqualTo("default@example.com")
    }

    @Test
    fun `null metadata returns default billing email without calling repository`() = runScenario(
        configuration = CONFIG_WITH_DEFAULT_EMAIL,
        customerMetadata = null,
    ) {
        assertThat(result).isEqualTo("default@example.com")
    }

    private fun runScenario(
        configuration: CommonConfiguration = CONFIG_WITHOUT_EMAIL,
        customerMetadata: CustomerMetadata? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val customerRepository = CallTrackingCustomerRepository()
        val retrieveEmail = DefaultRetrieveCustomerEmail(customerRepository)

        val result = retrieveEmail(
            configuration = configuration,
            customerMetadata = customerMetadata,
        )

        Scenario(
            result = result,
            customerRepository = customerRepository,
        ).block()

        customerRepository.ensureAllEventsConsumed()
    }

    private class Scenario(
        val result: String?,
        val customerRepository: CallTrackingCustomerRepository,
    )

    /**
     * A [FakeCustomerRepository] subclass that tracks [retrieveCustomer] calls via Turbine.
     * Returns null for all calls (Customer has an internal constructor in payments-core).
     */
    private class CallTrackingCustomerRepository : FakeCustomerRepository() {
        data class RetrieveCall(val customerId: String, val ephemeralKeySecret: String)

        val retrieveCalls = Turbine<RetrieveCall>()

        override suspend fun retrieveCustomer(
            customerId: String,
            ephemeralKeySecret: String,
        ) = null.also {
            retrieveCalls.add(RetrieveCall(customerId, ephemeralKeySecret))
        }

        fun ensureAllEventsConsumed() {
            retrieveCalls.ensureAllEventsConsumed()
        }
    }

    private companion object {
        val CONFIG_WITH_DEFAULT_EMAIL = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
        ).newBuilder()
            .defaultBillingDetails(PaymentSheet.BillingDetails(email = "default@example.com"))
            .build()
            .asCommonConfiguration()

        val CONFIG_WITHOUT_EMAIL = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
        ).asCommonConfiguration()

        val CUSTOMER_SESSION_METADATA = CustomerMetadata.CustomerSession(
            id = "cus_1",
            ephemeralKeySecret = "ek_test_session",
            customerSessionClientSecret = "css_test_123",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.None,
            saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
            canRemoveLastPaymentMethod = false,
            canUpdateFullPaymentMethodDetails = false,
        )

        val LEGACY_EK_METADATA = CustomerMetadata.LegacyEphemeralKey(
            id = "cus_123",
            ephemeralKeySecret = PaymentSheetFixtures.DEFAULT_EPHEMERAL_KEY,
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canUpdateFullPaymentMethodDetails = false,
        )

        val CHECKOUT_SESSION_METADATA = CustomerMetadata.CheckoutSession(
            sessionId = "cs_test_123",
            customerId = "cus_test_123",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.None,
            saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
            canRemoveLastPaymentMethod = false,
            canUpdateFullPaymentMethodDetails = false,
        )
    }
}
