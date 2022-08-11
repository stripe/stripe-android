package com.stripe.android.ui.core.forms.resources

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.address.AddressRepository

/**
 * [StaticAddressResourceRepository] that receives address resources pre-loaded.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StaticAddressResourceRepository(
    private val addressRepository: AddressRepository = AddressRepository(null)
) : ResourceRepository<AddressRepository> {
    override suspend fun waitUntilLoaded() {
        // Nothing to do since everything is pre-loaded
    }

    override fun isLoaded() = true

    override fun getRepository(): AddressRepository {
        return addressRepository
    }
}
