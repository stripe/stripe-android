package com.stripe.android.customersheet.data

import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.util.getDefaultPaymentMethodAsPaymentSelection
import com.stripe.android.customersheet.util.getDefaultPaymentMethodsEnabledForCustomerSheet
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.toSavedSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class CustomerSessionSavedSelectionDataSource @Inject constructor(
    private val elementsSessionManager: CustomerSessionElementsSessionManager,
    private val customerRepository: CustomerRepository,
    private val prefsRepositoryFactory: @JvmSuppressWildcards (String) -> PrefsRepository,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSheetSavedSelectionDataSource {
    override suspend fun retrieveSavedSelection(
        customerSessionElementsSession: CustomerSessionElementsSession?,
    ): CustomerSheetDataResult<SavedSelection?> {
        return withContext(workContext) {
            val loadedElementsSession = customerSessionElementsSession?.let {
                Result.success(it)
            } ?: elementsSessionManager.fetchElementsSession()
            return@withContext loadedElementsSession.fold(
                onSuccess = {
                    if (getDefaultPaymentMethodsEnabledForCustomerSheet(it.elementsSession)) {
                        useDefaultPaymentMethodFromBackend(it.customer)
                    } else {
                        useLocallySavedSelection()
                    }
                },
                onFailure = {
                    CustomerSheetDataResult.failure(it, displayMessage = null)
                }
            )
        }
    }

    private fun useDefaultPaymentMethodFromBackend(
        customer: ElementsSession.Customer,
    ): CustomerSheetDataResult<SavedSelection?> {
        val savedSelection = getDefaultPaymentMethodAsPaymentSelection(
            paymentMethods = customer.paymentMethods,
            defaultPaymentMethodId = customer.defaultPaymentMethod
        )?.toSavedSelection()
        return CustomerSheetDataResult.success(savedSelection)
    }

    private suspend fun useLocallySavedSelection(): CustomerSheetDataResult<SavedSelection?> {
        return createPrefsRepository().mapCatching { prefsRepository ->
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

    override suspend fun setSavedSelection(
        selection: SavedSelection?,
        shouldSyncDefault: Boolean,
    ): CustomerSheetDataResult<Unit> {
        return withContext(workContext) {
            elementsSessionManager.fetchCustomerSessionEphemeralKey().mapCatching { ephemeralKey ->
                if (shouldSyncDefault) {
                    saveSelectionToBackend(ephemeralKey, selection)
                } else {
                    saveSelectionToPrefs(selection)
                }
            }.toCustomerSheetDataResult()
        }
    }

    private suspend fun saveSelectionToPrefs(
        selection: SavedSelection?
    ) {
        createPrefsRepository().mapCatching { prefsRepository ->
            val result = prefsRepository.setSavedSelection(selection)

            if (!result) {
                throw IOException("Unable to persist payment option $selection")
            }
        }
    }

    private suspend fun saveSelectionToBackend(
        ephemeralKey: CachedCustomerEphemeralKey,
        selection: SavedSelection?
    ) {
        val paymentMethodId = (selection as? SavedSelection.PaymentMethod)?.id
        customerRepository.setDefaultPaymentMethod(
            paymentMethodId = paymentMethodId,
            customerInfo = CustomerRepository.CustomerInfo(
                id = ephemeralKey.customerId,
                ephemeralKeySecret = ephemeralKey.ephemeralKey,
                customerSessionClientSecret = ephemeralKey.customerSessionClientSecret,
            )
        ).getOrThrow()
    }

    private suspend fun createPrefsRepository(): CustomerSheetDataResult<PrefsRepository> {
        return elementsSessionManager.fetchCustomerSessionEphemeralKey().mapCatching { ephemeralKey ->
            prefsRepositoryFactory(ephemeralKey.customerId)
        }.toCustomerSheetDataResult()
    }
}
