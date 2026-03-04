package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultSavedPaymentMethodRepositoryTest {

    @Test
    fun `detach routes to checkout session repository when access is CheckoutSession`() = runScenario(
        checkoutSessionDetachResult = Result.success(FAKE_CHECKOUT_SESSION_RESPONSE),
        access = CHECKOUT_SESSION_ACCESS,
    ) {
        val result = repository.detachPaymentMethod(
            access = access,
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
    fun `detach routes to customer repository when access is Customer`() = runScenario(
        access = CUSTOMER_ACCESS,
    ) {
        val result = repository.detachPaymentMethod(
            access = access,
            paymentMethodId = "pm_123",
            canRemoveDuplicates = true,
        )

        assertThat(result.isSuccess).isTrue()

        val detachRequest = customerRepository.detachRequests.awaitItem()
        assertThat(detachRequest.paymentMethodId).isEqualTo("pm_123")
        assertThat(detachRequest.canRemoveDuplicates).isTrue()
    }

    @Test
    fun `detach propagates failure from checkout session repository`() = runScenario(
        checkoutSessionDetachResult = Result.failure(RuntimeException("Detach failed")),
        access = CHECKOUT_SESSION_ACCESS,
    ) {
        val result = repository.detachPaymentMethod(
            access = access,
            paymentMethodId = "pm_123",
            canRemoveDuplicates = false,
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("Detach failed")
    }

    private fun runScenario(
        access: SavedPaymentMethodAccess = CUSTOMER_ACCESS,
        checkoutSessionDetachResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val customerRepository = FakeCustomerRepository(
            paymentMethods = listOf(PaymentMethod.Builder().setId("pm_123").build()),
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
            access = access,
        ).block()
    }

    private class Scenario(
        val repository: DefaultSavedPaymentMethodRepository,
        val customerRepository: FakeCustomerRepository,
        val checkoutSessionRepository: FakeCheckoutSessionRepository,
        val access: SavedPaymentMethodAccess,
    )

    companion object {
        private val FAKE_CHECKOUT_SESSION_RESPONSE = CheckoutSessionResponse(
            id = "cs_123",
            amount = 1000,
            currency = "usd",
        )

        private val CHECKOUT_SESSION_ACCESS = SavedPaymentMethodAccess.CheckoutSession(
            sessionId = "cs_123",
        )

        private val CUSTOMER_ACCESS = SavedPaymentMethodAccess.Customer(
            id = "cus_456",
            ephemeralKeySecret = "ek_456",
            customerSessionClientSecret = "css_456",
        )
    }
}
