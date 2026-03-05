package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import org.mockito.kotlin.mock
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultSavedPaymentMethodRepositoryTest {

    @Test
    fun `detach routes to checkout session repository when customer is CheckoutSession`() = runScenario(
        checkoutSessionDetachResult = Result.success(FAKE_CHECKOUT_SESSION_RESPONSE),
        customerMetadata = CHECKOUT_SESSION_METADATA,
    ) {
        val result = repository.detachPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
            canRemoveDuplicates = false,
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().id).isEqualTo("pm_123")

        val detachRequest = checkoutSessionRepository.detachRequests.awaitItem()
        assertThat(detachRequest.sessionId).isEqualTo("cs_123")
        assertThat(detachRequest.paymentMethodId).isEqualTo("pm_123")
    }

    @Test
    fun `detach routes to customer repository when customer is Session`() = runScenario(
        customerMetadata = SESSION_METADATA,
    ) {
        val result = repository.detachPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
            canRemoveDuplicates = true,
        )

        assertThat(result.isSuccess).isTrue()

        val detachRequest = customerRepository.detachRequests.awaitItem()
        assertThat(detachRequest.paymentMethodId).isEqualTo("pm_123")
        assertThat(detachRequest.canRemoveDuplicates).isTrue()
    }

    @Test
    fun `detach routes to customer repository when customer is LegacyEphemeralKey`() = runScenario(
        customerMetadata = LEGACY_METADATA,
    ) {
        val result = repository.detachPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
            canRemoveDuplicates = false,
        )

        assertThat(result.isSuccess).isTrue()

        val detachRequest = customerRepository.detachRequests.awaitItem()
        assertThat(detachRequest.paymentMethodId).isEqualTo("pm_123")
        assertThat(detachRequest.canRemoveDuplicates).isFalse()
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
    fun `detach propagates failure from checkout session repository`() = runScenario(
        checkoutSessionDetachResult = Result.failure(RuntimeException("Detach failed")),
        customerMetadata = CHECKOUT_SESSION_METADATA,
    ) {
        val result = repository.detachPaymentMethod(
            customerMetadata = customerMetadata,
            paymentMethodId = "pm_123",
            canRemoveDuplicates = false,
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("Detach failed")
    }

    private fun runScenario(
        customerMetadata: CustomerMetadata = SESSION_METADATA,
        checkoutSessionDetachResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
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
        )
        val checkoutSessionRepository = FakeCheckoutSessionRepository(
            detachResult = checkoutSessionDetachResult,
        )
        val repository = DefaultSavedPaymentMethodRepository(
            customerRepository = customerRepository,
            checkoutSessionRepository = checkoutSessionRepository,
        )

        Scenario(
            repository = repository,
            customerRepository = customerRepository,
            checkoutSessionRepository = checkoutSessionRepository,
            customerMetadata = customerMetadata,
        ).block()
    }

    private class Scenario(
        val repository: DefaultSavedPaymentMethodRepository,
        val customerRepository: FakeCustomerRepository,
        val checkoutSessionRepository: FakeCheckoutSessionRepository,
        val customerMetadata: CustomerMetadata,
    )

    companion object {
        private val FAKE_CHECKOUT_SESSION_RESPONSE = CheckoutSessionResponse(
            id = "cs_123",
            amount = 1000,
            currency = "usd",
        )

        private val CHECKOUT_SESSION_METADATA = CustomerMetadata.CheckoutSession(
            sessionId = "cs_123",
            customerId = "cus_123",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
            canRemoveLastPaymentMethod = false,
            canRemoveDuplicates = false,
            canUpdateFullPaymentMethodDetails = false,
        )

        private val SESSION_METADATA = CustomerMetadata.CustomerSession(
            id = "cus_456",
            ephemeralKeySecret = "ek_456",
            customerSessionClientSecret = "css_456",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canRemoveDuplicates = true,
            canUpdateFullPaymentMethodDetails = false,
        )

        private val LEGACY_METADATA = CustomerMetadata.LegacyEphemeralKey(
            id = "cus_789",
            ephemeralKeySecret = "ek_789",
            isPaymentMethodSetAsDefaultEnabled = false,
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
            canRemoveLastPaymentMethod = true,
            canRemoveDuplicates = true,
            canUpdateFullPaymentMethodDetails = false,
        )
    }
}
