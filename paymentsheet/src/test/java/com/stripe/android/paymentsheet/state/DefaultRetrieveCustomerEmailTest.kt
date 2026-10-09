package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.Customer
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.CustomerFactory
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(TestParameterInjector::class)
internal class DefaultRetrieveCustomerEmailTest {

    @Test
    fun `default billing email takes priority and skips API call`() = runScenario(
        configuration = CONFIG_WITH_DEFAULT_EMAIL,
        customerMetadata = CUSTOMER_SESSION_METADATA,
    ) {
        assertThat(result).isEqualTo("default@example.com")
    }

    @Test
    fun `legacy ephemeral key calls repository with legacy credentials when no default email`() = runScenario(
        configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
        customerMetadata = LEGACY_EK_METADATA,
    ) {
        assertThat(result).isNull()
        val call = customerRepository.retrieveCustomerRequests.awaitItem()
        assertThat(call.customerId).isEqualTo("cus_123")
        assertThat(call.ephemeralKeySecret).isEqualTo(PaymentSheetFixtures.DEFAULT_EPHEMERAL_KEY)
        assertThat(call.apiConfiguration).isEqualTo(DEFAULT_API_CONFIG)
    }

    @Test
    fun `Checkout customer metadata without Checkout initialization uses billing email`() = runScenario(
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

    @Test
    fun `configuration email preferred over customer email`() = runTest {
        val customerEmail = "customer@stripe.com"
        runScenario(
            CONFIG_WITH_DEFAULT_EMAIL,
            customerEmail = customerEmail,
            customerMetadata = CUSTOMER_SESSION_METADATA,
        ) {
            assertThat(result).isEqualTo("default@example.com")
        }
    }

    @Test
    fun `customerEmail used when no configuration email`() = runTest {
        val customerEmail = "customer@stripe.com"
        runScenario(
            CONFIG_WITHOUT_EMAIL,
            customerEmail = customerEmail,
            customerMetadata = CUSTOMER_SESSION_METADATA,
        ) {
            assertThat(result).isEqualTo(customerEmail)
        }
    }

    @Test
    fun `when using customer sessions, no customer email if config email and customerEmail are null`() = runScenario(
        CONFIG_WITHOUT_EMAIL,
        customerEmail = null,
        customerMetadata = CUSTOMER_SESSION_METADATA,
    ) {
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `Checkout resolves email independently of customer metadata and billing defaults`(
        @TestParameter source: CheckoutEmailSource,
        @TestParameter hasCustomerMetadata: Boolean,
    ) = runScenario(
        configuration = CONFIG_WITH_DEFAULT_EMAIL,
        customerMetadata = CHECKOUT_SESSION_METADATA.takeIf { hasCustomerMetadata },
        customerEmail = "elements@example.com",
        initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = "test",
            collectedEmail = "collected@example.com".takeUnless { source == CheckoutEmailSource.Absent },
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                customerEmail = "session@example.com".takeIf { source == CheckoutEmailSource.Session },
                customer = CheckoutSessionResponse.Customer(
                    id = "cus_test",
                    email = "attached@example.com".takeIf {
                        source == CheckoutEmailSource.Session || source == CheckoutEmailSource.Attached
                    },
                    paymentMethods = emptyList(),
                    canDetachPaymentMethod = false,
                ),
            ),
        ),
    ) {
        assertThat(result).isEqualTo(source.email)
        customerRepository.retrieveCustomerRequests.expectNoEvents()
    }

    enum class CheckoutEmailSource(val email: String?) {
        Session("session@example.com"), Attached("attached@example.com"),
        Collected("collected@example.com"), Absent(null)
    }

    @Test
    fun `billing email skips legacy retrieval`() = runScenario(
        configuration = CONFIG_WITH_DEFAULT_EMAIL,
        customerMetadata = LEGACY_EK_METADATA,
    ) {
        assertThat(result).isEqualTo("default@example.com")
        customerRepository.retrieveCustomerRequests.expectNoEvents()
    }

    @Test
    fun `legacy retrieved customer supplies email`() = runScenario(
        customerMetadata = LEGACY_EK_METADATA,
        customer = CustomerFactory.create(email = "legacy@example.com"),
    ) {
        assertThat(result).isEqualTo("legacy@example.com")
        val call = customerRepository.retrieveCustomerRequests.awaitItem()
        assertThat(call.customerId).isEqualTo(LEGACY_EK_METADATA.id)
        assertThat(call.ephemeralKeySecret).isEqualTo(LEGACY_EK_METADATA.ephemeralKeySecret)
        assertThat(call.apiConfiguration).isEqualTo(DEFAULT_API_CONFIG)
    }

    private fun runScenario(
        configuration: CommonConfiguration = CONFIG_WITHOUT_EMAIL,
        customerMetadata: CustomerMetadata? = null,
        customerEmail: String? = null,
        customer: Customer? = null,
        initializationMode: PaymentElementLoader.InitializationMode =
            PaymentElementLoader.InitializationMode.PaymentIntent("pi_test_secret_test"),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val customerRepository = FakeCustomerRepository(customer = customer)
        val retrieveEmail = DefaultRetrieveCustomerEmail(
            customerRepository,
            FakeDurationProvider(),
        )

        val result = retrieveEmail(
            configuration = configuration,
            initializationMode = initializationMode,
            customerMetadata = customerMetadata,
            customerEmail = customerEmail,
            apiConfiguration = DEFAULT_API_CONFIG,
        )

        Scenario(
            result = result,
            customerRepository = customerRepository,
        ).block()

        customerRepository.ensureAllEventsConsumed()
    }

    private class Scenario(
        val result: String?,
        val customerRepository: FakeCustomerRepository,
    )

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
            canUpdateCardExpiryAndBillingDetails = false,
        )

        val LEGACY_EK_METADATA = CustomerMetadata.LegacyEphemeralKey(
            id = "cus_123",
            ephemeralKeySecret = PaymentSheetFixtures.DEFAULT_EPHEMERAL_KEY,
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canUpdateCardExpiryAndBillingDetails = false,
        )

        val CHECKOUT_SESSION_METADATA = CustomerMetadata.CheckoutSession(
            sessionId = "cs_test_123",
            customerId = "cus_test_123",
            removePaymentMethod = PaymentMethodRemovePermission.None,
            saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
        )
    }
}
