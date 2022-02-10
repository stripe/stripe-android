package com.stripe.android.ui.core.forms.resources

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import com.stripe.android.ui.core.elements.BankRepository

/**
 * Interface that provides all resources needed by the forms.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ResourceRepository {
    /**
     * Suspend function that will wait for all resources to be loaded.
     * Must be called before trying to get any of the repositories.
     */
    suspend fun waitUntilLoaded()

    fun isLoaded(): Boolean

    fun getBankRepository(): BankRepository
    fun getAddressRepository(): AddressFieldElementRepository
}
