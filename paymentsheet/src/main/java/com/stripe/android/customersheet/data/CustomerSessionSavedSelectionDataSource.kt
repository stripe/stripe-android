package com.stripe.android.customersheet.data

import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class CustomerSessionSavedSelectionDataSource @Inject constructor(
    private val elementsSessionManager: CustomerSessionElementsSessionManager,
    private val prefsRepositoryFactory: @JvmSuppressWildcards (String) -> PrefsRepository,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSheetSavedSelectionDataSource {
    override suspend fun retrieveSavedSelection(): CustomerSheetDataResult<SavedSelection?> {
        return withContext(workContext) {
            createPrefsRepository().mapCatching { prefsRepository ->
                prefsRepository.getSavedSelection(
                    /*
                     * We don't calculate on `Google Pay` availability in this function. Instead, we check
                     * within `CustomerSheet` similar to how we check if a saved payment option is still exists
                     * within the user's payment methods from `retrievePaymentMethods`
                     */
                    isGooglePayAvailable = true,
                    isLinkAvailable = false,
                )
            }
        }
    }

    override suspend fun setSavedSelection(selection: SavedSelection?): CustomerSheetDataResult<Unit> {
        return withContext(workContext) {
            createPrefsRepository().mapCatching { prefsRepository ->
                val result = prefsRepository.setSavedSelection(selection)

                if (!result) {
                    throw IOException("Unable to persist payment option $selection")
                }
            }
        }
    }

    private suspend fun createPrefsRepository(): CustomerSheetDataResult<PrefsRepository> {
        return elementsSessionManager.fetchCustomerSessionEphemeralKey().mapCatching { ephemeralKey ->
            prefsRepositoryFactory(ephemeralKey.customerId)
        }.toCustomerSheetDataResult()
    }
}
