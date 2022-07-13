package com.stripe.android.ui.core.forms.resources

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.address.AddressFieldElementRepository

/**
 * [ResourceRepository] that receives all resources pre-loaded.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StaticResourceRepository(
    private val addressRepository: AddressFieldElementRepository,
    private val lpmRepository: LpmRepository = LpmRepository(LpmRepository.LpmRepositoryArguments(null))
) : ResourceRepository {
    // Nothing to do since everything is pre-loaded
    override suspend fun waitUntilLoaded() = true

    override fun isLoaded() = true

    override fun getLpmRepository(): LpmRepository {
        return lpmRepository
    }

    override fun getAddressRepository(): AddressFieldElementRepository {
        return addressRepository
    }
}
