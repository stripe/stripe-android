package com.stripe.android.paymentsheet.forms.resources

import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.BankRepository

/**
 * Interface that provides all resources needed by the forms.
 */
internal interface ResourceRepository {
    suspend fun getBankRepository(): BankRepository
    suspend fun getAddressRepository(): AddressFieldElementRepository
}
