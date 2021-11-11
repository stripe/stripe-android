package com.stripe.android.paymentsheet.forms.resources

import android.content.res.Resources
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.BankRepository
import com.stripe.android.view.CountryUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
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
    private lateinit var bankRepository: BankRepository
    private lateinit var addressRepository: AddressFieldElementRepository

    private val loadingJobs: MutableList<Job> = mutableListOf()

    init {
        loadingJobs.add(
            CoroutineScope(workContext).launch {
                bankRepository = BankRepository(resources)
            }
        )
        loadingJobs.add(
            CoroutineScope(workContext).launch {
                addressRepository = AddressFieldElementRepository(resources)
            }
        )
        loadingJobs.add(
            CoroutineScope(workContext).launch {
                // Countries are also used outside of payment sheet.
                // This will initialize the list, sort it and cache it for the given locale.
                CountryUtils.getOrderedCountries(locale ?: Locale.US)
            }
        )
    }

    override suspend fun waitUntilLoaded() {
        loadingJobs.joinAll()
        loadingJobs.clear()
    }

    override fun isLoaded() = loadingJobs.isEmpty()

    override fun getBankRepository() = bankRepository
    override fun getAddressRepository() = addressRepository
}
