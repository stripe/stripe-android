package com.stripe.android.paymentsheet.forms.resources

import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.BankRepository

/**
 * [ResourceRepository] that receives all resources pre-loaded.
 */
internal class StaticResourceRepository(
    private val bankRepository: BankRepository,
    private val addressRepository: AddressFieldElementRepository
) : ResourceRepository {
    override suspend fun getBankRepository(): BankRepository {
        return bankRepository
    }

    override suspend fun getAddressRepository(): AddressFieldElementRepository {
        return addressRepository
    }
}
