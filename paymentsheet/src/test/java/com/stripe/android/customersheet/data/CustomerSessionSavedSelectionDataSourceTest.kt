package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test
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

        val result = dataSource.setSavedSelection(SavedSelection.PaymentMethod(id = "pm_1"))

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
            ephemeralKey = Result.failure(exception)
        )
        val dataSource = createDataSource(
            elementsSessionManager = elementsSessionManager,
        )

        val result = dataSource.setSavedSelection(SavedSelection.PaymentMethod(id = "pm_1"))

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<Unit>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
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

        val result = dataSource.retrieveSavedSelection(elementsSession = null)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<Unit>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `When default payment methods enabled, should get selection from backend`() {
        // TODO: implement test.
    }

    @Test
    fun `When default payment methods enabled and no default PM, should return null selection`() {
        // TODO: implement test.
    }

    @Test
    fun `When elements session passed to retrieveSavedSelection, should not re-query elements session`() {
        // TODO: implement test.
    }

    private suspend fun createDataSource(
        elementsSessionManager: CustomerSessionElementsSessionManager = FakeCustomerSessionElementsSessionManager(),
        prefsRepository: PrefsRepository = FakePrefsRepository(),
    ): CustomerSheetSavedSelectionDataSource {
        return CustomerSessionSavedSelectionDataSource(
            elementsSessionManager = elementsSessionManager,
            prefsRepositoryFactory = {
                prefsRepository
            },
            workContext = coroutineContext
        )
    }
}
