package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultSavedPaymentMethodRepositoryTest {

    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `detach routes to checkout session repository when customer is CheckoutSession`() = runScenario(
        customerMetadata = CHECKOUT_SESSION_METADATA,
    ) {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/cs_123"),
            bodyPart("payment_method_to_detach", "pm_123"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = repository.detachPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().id).isEqualTo("pm_123")
    }

    @Test
    fun `detach routes to customer repository when customer is Session`() = runScenario(
        customerMetadata = SESSION_METADATA,
    ) {
        val result = repository.detachPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
        )

        assertThat(result.isSuccess).isTrue()

        val detachRequest = customerRepository.detachRequests.awaitItem()
        assertThat(detachRequest.paymentMethodId).isEqualTo("pm_123")
        assertThat(detachRequest.customerSessionClientSecret).isEqualTo("css_456")
    }

    @Test
    fun `detach routes to customer repository when customer is LegacyEphemeralKey`() = runScenario(
        customerMetadata = LEGACY_METADATA,
    ) {
        val result = repository.detachPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
        )

        assertThat(result.isSuccess).isTrue()

        val detachRequest = customerRepository.detachRequests.awaitItem()
        assertThat(detachRequest.paymentMethodId).isEqualTo("pm_123")
        assertThat(detachRequest.customerSessionClientSecret).isNull()
    }

    @Test
    fun `update routes to customer repository when customer is CustomerSession`() = runScenario(
        customerMetadata = SESSION_METADATA,
    ) {
        val result = repository.updatePaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
            params = PaymentMethodUpdateParams.createCard(),
        )

        assertThat(result.isSuccess).isTrue()

        val updateRequest = customerRepository.updateRequests.awaitItem()
        assertThat(updateRequest.paymentMethodId).isEqualTo("pm_123")
    }

    @Test
    fun `update fails for checkout session`() = runScenario(
        customerMetadata = CHECKOUT_SESSION_METADATA,
    ) {
        val result = repository.updatePaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
            params = PaymentMethodUpdateParams.createCard(),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NotImplementedError::class.java)
    }

    @Test
    fun `update routes to customer repository when customer is LegacyEphemeralKey`() = runScenario(
        customerMetadata = LEGACY_METADATA,
    ) {
        val result = repository.updatePaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
            params = PaymentMethodUpdateParams.createCard(),
        )

        assertThat(result.isSuccess).isTrue()

        val updateRequest = customerRepository.updateRequests.awaitItem()
        assertThat(updateRequest.paymentMethodId).isEqualTo("pm_123")
    }

    @Test
    fun `setDefault routes to customer repository when customer is CustomerSession`() = runScenario(
        customerMetadata = SESSION_METADATA,
    ) {
        val result = repository.setDefaultPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
        )

        assertThat(result.isSuccess).isTrue()

        val setDefaultRequest = customerRepository.setDefaultPaymentMethodRequests.awaitItem()
        assertThat(setDefaultRequest.paymentMethodId).isEqualTo("pm_123")
    }

    @Test
    fun `setDefault fails for checkout session`() = runScenario(
        customerMetadata = CHECKOUT_SESSION_METADATA,
    ) {
        val result = repository.setDefaultPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NotImplementedError::class.java)
    }

    @Test
    fun `setDefault routes to customer repository when customer is LegacyEphemeralKey`() = runScenario(
        customerMetadata = LEGACY_METADATA,
    ) {
        val result = repository.setDefaultPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
        )

        assertThat(result.isSuccess).isTrue()

        val setDefaultRequest = customerRepository.setDefaultPaymentMethodRequests.awaitItem()
        assertThat(setDefaultRequest.paymentMethodId).isEqualTo("pm_123")
    }

    @Test
    fun `retrievePaymentMethod routes to customer repository when customer is CustomerSession`() = runScenario(
        customerMetadata = SESSION_METADATA,
        retrievePaymentMethodResult = Result.success(
            PaymentMethod.Builder().setId("pm_123").build()
        ),
    ) {
        val result = repository.retrievePaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().id).isEqualTo("pm_123")
    }

    @Test
    fun `retrievePaymentMethod routes to customer repository when customer is LegacyEphemeralKey`() = runScenario(
        customerMetadata = LEGACY_METADATA,
        retrievePaymentMethodResult = Result.success(
            PaymentMethod.Builder().setId("pm_456").build()
        ),
    ) {
        val result = repository.retrievePaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_456",
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().id).isEqualTo("pm_456")
    }

    @Test
    fun `retrievePaymentMethod fails for checkout session`() = runScenario(
        customerMetadata = CHECKOUT_SESSION_METADATA,
    ) {
        val result = repository.retrievePaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NotImplementedError::class.java)
    }

    @Test
    fun `detach propagates failure from checkout session repository`() = runScenario(
        customerMetadata = CHECKOUT_SESSION_METADATA,
    ) {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/cs_123"),
            bodyPart("payment_method_to_detach", "pm_123"),
        ) { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Detach failed"}}""")
        }

        val result = repository.detachPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
        )

        assertThat(result.isFailure).isTrue()
    }

    private fun runScenario(
        customerMetadata: CustomerMetadata = SESSION_METADATA,
        retrievePaymentMethodResult: Result<PaymentMethod> = Result.failure(NotImplementedError()),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val customerRepository = FakeCustomerRepository(
            paymentMethods = listOf(PaymentMethod.Builder().setId("pm_123").build()),
            onUpdatePaymentMethod = {
                Result.success(PaymentMethod.Builder().setId("pm_123").build())
            },
            onSetDefaultPaymentMethod = {
                // Customer has an internal constructor, so we use mock() here.
                // The Customer value is never inspected in these tests.
                Result.success(mock())
            },
            onRetrievePaymentMethod = { _ -> retrievePaymentMethodResult },
        )
        val checkoutSessionRepository = CheckoutSessionRepository(
            clientParams = ElementsSessionClientParams(
                mobileAppId = "com.stripe.android.test",
                mobileSessionIdProvider = { "test_session" },
            ),
            stripeNetworkClient = DefaultStripeNetworkClient(),
            publishableKeyProvider = { "pk_test_123" },
            stripeAccountIdProvider = { null },
        )
        val repository = DefaultSavedPaymentMethodRepository(
            customerRepository = customerRepository,
            checkoutSessionRepository = checkoutSessionRepository,
        )

        Scenario(
            repository = repository,
            customerRepository = customerRepository,
            customerMetadata = customerMetadata,
        ).block()
    }

    private class Scenario(
        val repository: DefaultSavedPaymentMethodRepository,
        val customerRepository: FakeCustomerRepository,
        val customerMetadata: CustomerMetadata,
    )

    companion object {
        private val CHECKOUT_SESSION_METADATA = CustomerMetadata.CheckoutSession(
            sessionId = "cs_123",
            customerId = "cus_123",
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
        )

        private val SESSION_METADATA = CustomerMetadata.CustomerSession(
            id = "cus_456",
            ephemeralKeySecret = "ek_456",
            customerSessionClientSecret = "css_456",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canUpdateFullPaymentMethodDetails = false,
        )

        private val LEGACY_METADATA = CustomerMetadata.LegacyEphemeralKey(
            id = "cus_789",
            ephemeralKeySecret = "ek_789",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canUpdateFullPaymentMethodDetails = false,
        )
    }
}
