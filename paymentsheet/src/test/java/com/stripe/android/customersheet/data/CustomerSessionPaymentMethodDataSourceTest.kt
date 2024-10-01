package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.coroutines.coroutineContext

class CustomerSessionPaymentMethodDataSourceTest {
    @Test
    fun `on fetch payment methods, should use payment methods from elements sessions response`() = runTest {
        val paymentMethods = PaymentMethodFactory.cards(size = 6)
        val paymentMethodDataSource = createPaymentMethodDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                paymentMethods = paymentMethods,
            ),
        )

        val paymentMethodsResult = paymentMethodDataSource.retrievePaymentMethods()

        assertThat(paymentMethodsResult).isInstanceOf<CustomerSheetDataResult.Success<List<PaymentMethod>>>()

        val returnedPaymentMethods = paymentMethodsResult.asSuccess().value

        assertThat(returnedPaymentMethods).containsExactlyElementsIn(paymentMethods)
    }

    @Test
    fun `on fetch payment methods, should fail if elements session fetch fails`() = runTest {
        val exception = IllegalStateException("Failed to load!")
        val paymentMethodDataSource = createPaymentMethodDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                elementsSession = Result.failure(exception)
            ),
        )

        val paymentMethodsResult = paymentMethodDataSource.retrievePaymentMethods()

        assertThat(paymentMethodsResult).isInstanceOf<CustomerSheetDataResult.Failure<List<PaymentMethod>>>()

        val failedResult = paymentMethodsResult.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on attach, should fail & report since attach is not support for customer session`() = runTest {
        val errorReporter = FakeErrorReporter()
        val paymentMethodDataSource = createPaymentMethodDataSource(
            errorReporter = errorReporter,
        )

        val attachResult = paymentMethodDataSource.attachPaymentMethod(paymentMethodId = "pm_1")

        assertThat(attachResult).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = attachResult.asFailure()

        assertThat(failedResult.cause).isInstanceOf<IllegalStateException>()
        assertThat(failedResult.cause.message).isEqualTo(
            "'attach' is not supported for `CustomerSession`!"
        )

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.UnexpectedErrorEvent.CUSTOMER_SHEET_ATTACH_CALLED_WITH_CUSTOMER_SESSION.eventName
        )
    }

    @Test
    fun `on detach, should detach from customer repository`() = runTest {
        val paymentMethod = PaymentMethodFactory.card(id = "pm_1")
        val customerRepository = FakeCustomerRepository(
            onDetachPaymentMethod = {
                Result.success(paymentMethod)
            }
        )

        val paymentMethodDataSource = createPaymentMethodDataSource(
            customerRepository = customerRepository,
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                ephemeralKey = Result.success(
                    CachedCustomerEphemeralKey(
                        customerId = "cus_1",
                        ephemeralKey = "ek_123",
                        expiresAt = 999999,
                    )
                )
            )
        )

        val result = paymentMethodDataSource.detachPaymentMethod(paymentMethodId = "pm_1")

        val detachRequest = customerRepository.detachRequests.awaitItem()

        assertThat(detachRequest.paymentMethodId).isEqualTo("pm_1")
        assertThat(detachRequest.customerInfo.id).isEqualTo("cus_1")
        assertThat(detachRequest.customerInfo.ephemeralKeySecret).isEqualTo("ek_123")
        assertThat(detachRequest.canRemoveDuplicates).isTrue()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<PaymentMethod>>()

        val removedPaymentMethod = result.asSuccess().value

        assertThat(removedPaymentMethod).isEqualTo(paymentMethod)
    }

    @Test
    fun `on detach, should fail if detach from customer repository fails`() = runTest {
        val exception = IllegalStateException("Failed to detach!")
        val customerRepository = FakeCustomerRepository(
            onDetachPaymentMethod = {
                Result.failure(exception)
            }
        )

        val paymentMethodDataSource = createPaymentMethodDataSource(
            customerRepository = customerRepository,
        )

        val result = paymentMethodDataSource.detachPaymentMethod(
            paymentMethodId = "pm_1",
        )

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on detach, should fail if ephemeral key fetch fails`() = runTest {
        val exception = IllegalStateException("Failed to fetch ephemeral key!")

        val paymentMethodDataSource = createPaymentMethodDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                ephemeralKey = Result.failure(exception)
            ),
        )

        val result = paymentMethodDataSource.detachPaymentMethod(
            paymentMethodId = "pm_1",
        )

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on update, should update from customer repository`() = runTest {
        val paymentMethod = PaymentMethodFactory.card(id = "pm_1")
        val customerRepository = FakeCustomerRepository(
            onUpdatePaymentMethod = {
                Result.success(paymentMethod)
            }
        )

        val paymentMethodDataSource = createPaymentMethodDataSource(
            customerRepository = customerRepository,
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                ephemeralKey = Result.success(
                    CachedCustomerEphemeralKey(
                        customerId = "cus_1",
                        ephemeralKey = "ek_123",
                        expiresAt = 999999,
                    )
                )
            )
        )

        val updateParams = PaymentMethodUpdateParams.createCard(
            expiryYear = 2027,
            expiryMonth = 7
        )

        val result = paymentMethodDataSource.updatePaymentMethod(
            paymentMethodId = "pm_1",
            params = updateParams
        )

        val detachRequest = customerRepository.updateRequests.awaitItem()

        assertThat(detachRequest.paymentMethodId).isEqualTo("pm_1")
        assertThat(detachRequest.customerInfo.id).isEqualTo("cus_1")
        assertThat(detachRequest.customerInfo.ephemeralKeySecret).isEqualTo("ek_123")
        assertThat(detachRequest.params).isEqualTo(updateParams)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<PaymentMethod>>()

        val updatedPaymentMethod = result.asSuccess().value

        assertThat(updatedPaymentMethod).isEqualTo(paymentMethod)
    }

    @Test
    fun `on update, should fail if update from customer repository fails`() = runTest {
        val exception = IllegalStateException("Failed to update!")
        val customerRepository = FakeCustomerRepository(
            onUpdatePaymentMethod = {
                Result.failure(exception)
            },
        )

        val paymentMethodDataSource = createPaymentMethodDataSource(
            customerRepository = customerRepository,
        )

        val result = paymentMethodDataSource.updatePaymentMethod(
            paymentMethodId = "pm_1",
            params = PaymentMethodUpdateParams.createCard(
                expiryYear = 2027,
                expiryMonth = 7,
            ),
        )

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on update, should fail if ephemeral key fetch fails`() = runTest {
        val exception = IllegalStateException("Failed to fetch ephemeral key!")

        val paymentMethodDataSource = createPaymentMethodDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                ephemeralKey = Result.failure(exception)
            ),
        )

        val result = paymentMethodDataSource.updatePaymentMethod(
            paymentMethodId = "pm_1",
            params = PaymentMethodUpdateParams.createCard(
                expiryYear = 2027,
                expiryMonth = 7,
            ),
        )

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    private suspend fun createPaymentMethodDataSource(
        elementsSessionManager: CustomerSessionElementsSessionManager = FakeCustomerSessionElementsSessionManager(),
        customerRepository: CustomerRepository = FakeCustomerRepository(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
    ): CustomerSheetPaymentMethodDataSource {
        return CustomerSessionPaymentMethodDataSource(
            elementsSessionManager = elementsSessionManager,
            customerRepository = customerRepository,
            errorReporter = errorReporter,
            workContext = coroutineContext,
        )
    }
}
