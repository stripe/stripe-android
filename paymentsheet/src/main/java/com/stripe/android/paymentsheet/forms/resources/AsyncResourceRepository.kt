package com.stripe.android.paymentsheet.forms.resources

import android.content.res.Resources
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.BankRepository
import com.stripe.android.view.CountryUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * [ResourceRepository] that loads all resources from JSON asynchronously.
 */
@Singleton
internal class AsyncResourceRepository @Inject constructor(
    private val resources: Resources?,
    @IOContext private val workContext: CoroutineContext,
    private val locale: Locale?
) : ResourceRepository {
    private val bankRepositoryMutex = Mutex()
    private lateinit var bankRepository: BankRepository

    private val addressRepositoryMutex = Mutex()
    private lateinit var addressRepository: AddressFieldElementRepository

    init {
        GlobalScope.launch(workContext) {
            getBankRepository()
        }
        GlobalScope.launch(workContext) {
            getAddressRepository()
        }
        GlobalScope.launch(workContext) {
            // Countries are also used outside of payment sheet.
            // This will initialize the list, sort it and cache it for the given locale.
            CountryUtils.getOrderedCountries(locale ?: Locale.US)
        }
    }

    override suspend fun getBankRepository(): BankRepository {
        if (!::bankRepository.isInitialized) {
            bankRepositoryMutex.withLock {
                if (!::bankRepository.isInitialized) {
                    bankRepository = BankRepository(resources)
                }
            }
        }
        return bankRepository
    }

    override suspend fun getAddressRepository(): AddressFieldElementRepository {
        if (!::addressRepository.isInitialized) {
            addressRepositoryMutex.withLock {
                if (!::addressRepository.isInitialized) {
                    addressRepository = AddressFieldElementRepository(resources)
                }
            }
        }
        return addressRepository
    }
}
