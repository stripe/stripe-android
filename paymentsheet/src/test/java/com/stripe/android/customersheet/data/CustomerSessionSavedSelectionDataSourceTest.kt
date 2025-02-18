package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import kotlin.coroutines.coroutineContext

class CustomerSessionSavedSelectionDataSourceTest {
    @Test
    fun `on fetch saved selection, should get selection from prefs repository`() = runTest {
        val prefsRepository = FakePrefsRepository().apply {
            setSavedSelection(SavedSelection.GooglePay)
        }

        val dataSource = createDataSource(
            prefsRepository = prefsRepository
        )

        val result = dataSource.retrieveSavedSelection(null)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<SavedSelection?>>()

        val successResult = result.asSuccess()

        assertThat(successResult.value).isEqualTo(SavedSelection.GooglePay)
    }

    @Test
    fun `on failed to fetch ephemeral key, should fail to get selection from prefs repository`() = runTest {
        val exception = IllegalStateException("Failed to load!")

        val elementsSessionManager = FakeCustomerSessionElementsSessionManager(
            ephemeralKey = Result.failure(exception)
        )
        val dataSource = createDataSource(
            elementsSessionManager = elementsSessionManager,
        )

        val result = dataSource.retrieveSavedSelection(null)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<SavedSelection?>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on set saved selection, should set selection in prefs repository`() = runTest {
        val prefsRepository = FakePrefsRepository()

        val dataSource = createDataSource(
            prefsRepository = prefsRepository
        )

        val result = dataSource.setSavedSelection(SavedSelection.PaymentMethod(id = "pm_1"), false)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<Unit>>()

        val savedSelection = prefsRepository.getSavedSelection(
            isGooglePayAvailable = false,
            isLinkAvailable = false
        )

        assertThat(savedSelection).isEqualTo(SavedSelection.PaymentMethod(id = "pm_1"))
    }

    @Test
    fun `on failed to fetch ephemeral key, should fail to set selection in prefs repository`() = runTest {
        val exception = IllegalStateException("Failed to load!")

        val elementsSessionManager = FakeCustomerSessionElementsSessionManager(
            ephemeralKey = Result.failure(exception),
        )
        val dataSource = createDataSource(
            elementsSessionManager = elementsSessionManager,
        )

        val result = dataSource.setSavedSelection(SavedSelection.PaymentMethod(id = "pm_1"), false)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<Unit>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `When default PMs feature is enabled, should set payment method in backend`() = runTest {
        val customerRepository = FakeCustomerRepository(
            onSetDefaultPaymentMethod = { Result.success(createCustomer()) }
        )
        val elementsSessionManager = FakeCustomerSessionElementsSessionManager(
            isPaymentMethodSyncDefaultEnabled = true,
        )
        val dataSource = createDataSource(
            customerRepository = customerRepository,
            elementsSessionManager = elementsSessionManager,
        )
        val expectedNewDefaultPaymentMethodId = "pm_1"

        val result = dataSource.setSavedSelection(
            SavedSelection.PaymentMethod(id = expectedNewDefaultPaymentMethodId),
            shouldSyncDefault = true
        )

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<Unit>>()
        val setDefaultRequest = customerRepository.setDefaultPaymentMethodRequests.awaitItem()
        assertThat(setDefaultRequest.paymentMethodId).isEqualTo(expectedNewDefaultPaymentMethodId)
    }

    @Test
    fun `When default PMs feature is enabled and retrieving from the backend fails, getting selection should fail`() =
        runTest {
            val expectedException = IllegalStateException("Failed to set payment method!")
            val customerRepository = FakeCustomerRepository(
                onSetDefaultPaymentMethod = { Result.failure(expectedException) }
            )
            val elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                isPaymentMethodSyncDefaultEnabled = true,
            )
            val dataSource = createDataSource(
                customerRepository = customerRepository,
                elementsSessionManager = elementsSessionManager,
            )

            val result = dataSource.setSavedSelection(
                SavedSelection.PaymentMethod(id = "pm_1"),
                shouldSyncDefault = true,
            )

            assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<Unit>>()

            val failedResult = result.asFailure()

            assertThat(failedResult.cause).isEqualTo(expectedException)
        }

    @Test
    fun `On failed to get elements session, should fail to get selection`() = runTest {
        val exception = IllegalStateException("Failed to load!")

        val elementsSessionManager = FakeCustomerSessionElementsSessionManager(
            elementsSession = Result.failure(exception),
        )
        val dataSource = createDataSource(
            elementsSessionManager = elementsSessionManager,
        )

        val result = dataSource.retrieveSavedSelection(customerSessionElementsSession = null)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<Unit>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `When default payment methods enabled, should get selection from backend`() = runTest {
        val expectedSavedSelectionId = "pm_1"
        val elementsSessionManager = FakeCustomerSessionElementsSessionManager(
            isPaymentMethodSyncDefaultEnabled = true,
            defaultPaymentMethodId = expectedSavedSelectionId,
            paymentMethods = listOf(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_2"),
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = expectedSavedSelectionId),
            )
        )

        val dataSource = createDataSource(
            elementsSessionManager = elementsSessionManager,
        )

        val result = dataSource.retrieveSavedSelection(customerSessionElementsSession = null)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<SavedSelection?>>()

        val successResult = result.asSuccess()

        assertThat(successResult.value).isEqualTo(SavedSelection.PaymentMethod(id = expectedSavedSelectionId))
    }

    @Test
    fun `When default payment methods enabled and no default PM, should return null selection`() = runTest {
        val elementsSessionManager = FakeCustomerSessionElementsSessionManager(
            isPaymentMethodSyncDefaultEnabled = true,
            defaultPaymentMethodId = null,
        )

        val dataSource = createDataSource(
            elementsSessionManager = elementsSessionManager,
        )

        val result = dataSource.retrieveSavedSelection(customerSessionElementsSession = null)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<SavedSelection?>>()

        val successResult = result.asSuccess()

        assertThat(successResult.value).isEqualTo(null)
    }

    @Test
    fun `When elements session passed to retrieveSavedSelection, should not re-query elements session`() = runTest {
        val existingElementsSession = FakeCustomerSessionElementsSessionManager().fetchElementsSession().getOrThrow()
        val failingElementsSessionManager = FakeCustomerSessionElementsSessionManager(
            elementsSession = Result.failure(
                IllegalAccessError("Should not re-query elements session in this test!")
            )
        )

        val dataSource = createDataSource(
            elementsSessionManager = failingElementsSessionManager
        )

        val result = dataSource.retrieveSavedSelection(customerSessionElementsSession = existingElementsSession)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<SavedSelection?>>()

        // No exception being thrown indicates we did not try to re-query elements session from the
        // "failingElementsSessionManager".
    }


    private suspend fun createDataSource(
        elementsSessionManager: CustomerSessionElementsSessionManager = FakeCustomerSessionElementsSessionManager(),
        customerRepository: FakeCustomerRepository = FakeCustomerRepository(),
        prefsRepository: PrefsRepository = FakePrefsRepository(),
    ): CustomerSheetSavedSelectionDataSource {
        return CustomerSessionSavedSelectionDataSource(
            elementsSessionManager = elementsSessionManager,
            customerRepository = customerRepository,
            prefsRepositoryFactory = {
                prefsRepository
            },
            workContext = coroutineContext
        )
    }

    private fun createCustomer(): Customer {
        // Using a mock here, because the Customer constructor is internal. If I make it visible,
        // we would have to expose the copy function for that class and it doesn't seem worth it, given
        // that we don't even use this object.
        return mock()
    }
}
