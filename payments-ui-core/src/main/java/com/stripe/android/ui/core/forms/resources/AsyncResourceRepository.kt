package com.stripe.android.ui.core.forms.resources

import android.content.res.Resources
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import com.stripe.android.ui.core.elements.LpmRepository
import com.stripe.android.ui.core.elements.StringRepository
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AsyncResourceRepository @Inject constructor(
    private val resources: Resources,
    @IOContext private val workContext: CoroutineContext,
    private val locale: Locale?
) : ResourceRepository {
    private lateinit var lpmRepository: LpmRepository
    private val stringRepository: StringRepository
    private lateinit var addressRepository: AddressFieldElementRepository

    private val loadingJobs: MutableList<Job> = mutableListOf()

    init {
        stringRepository = StringRepository(resources)

        lpmRepository = LpmRepository(resources)
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

    override fun getLpmRepository() = lpmRepository
    override fun getStringRepository() = stringRepository
    override fun getAddressRepository() = addressRepository
}
