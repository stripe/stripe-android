package com.stripe.android.ui.core.forms.resources

import android.content.res.Resources
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.ui.core.address.AddressFieldElementRepository
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
    private val locale: Locale?,
    analyticsRequestExecutor: DefaultAnalyticsRequestExecutor,
    paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
) : ResourceRepository {
    private val lpmRepository: LpmRepository = LpmRepository.getInstance(
        LpmRepository.LpmRepositoryArguments(
            resources = resources,
            analyticsRequestExecutor = analyticsRequestExecutor,
            paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory
        )
    )
    private lateinit var addressRepository: AddressFieldElementRepository

    private val loadingJobs: MutableList<Job> = mutableListOf()

    init {
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

        lpmRepository.waitUntilLoaded()
    }

    override fun isLoaded() = loadingJobs.isEmpty() && lpmRepository.isLoaded()

    override fun getLpmRepository() = lpmRepository
    override fun getAddressRepository() = addressRepository
}
