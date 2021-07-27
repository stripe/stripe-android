package com.stripe.android.paymentsheet.specifications

import com.stripe.android.paymentsheet.address.AddressFieldElementRepository

/**
 * This holds all the resources read in from JSON.
 */
internal class ResourceRepository(
    internal val bankRepository: BankRepository,
    internal val addressRepository: AddressFieldElementRepository
) {
    internal fun init() {
        bankRepository.init()
        addressRepository.init()
    }
}
