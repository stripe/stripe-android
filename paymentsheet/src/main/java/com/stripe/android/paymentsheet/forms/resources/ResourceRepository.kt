package com.stripe.android.paymentsheet.forms.resources

import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.BankRepository

/**
 * Interface that provides all resources needed by the forms.
 */
internal interface ResourceRepository {
    /**
     * Suspend function that will wait for all resources to be loaded.
     * Must be called before trying to get any of the repositories.
     */
    suspend fun waitUntilLoaded()

    fun isLoaded(): Boolean

    fun getBankRepository(): BankRepository
    fun getAddressRepository(): AddressFieldElementRepository
}
