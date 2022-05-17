package com.stripe.android.ui.core.forms.resources

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import com.stripe.android.ui.core.elements.LpmFormRepository

/**
 * [ResourceRepository] that receives all resources pre-loaded.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StaticResourceRepository(
    private val lpmFormRepository: LpmFormRepository,
    private val addressRepository: AddressFieldElementRepository
) : ResourceRepository {
    override suspend fun waitUntilLoaded() {
        // Nothing to do since everything is pre-loaded
    }

    override fun isLoaded() = true

    override fun getBankRepository(): LpmFormRepository {
        return lpmFormRepository
    }

    override fun getAddressRepository(): AddressFieldElementRepository {
        return addressRepository
    }
}
