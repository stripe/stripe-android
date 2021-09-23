package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This holds all the resources read in from JSON.
 */
@Singleton
internal class ResourceRepository @Inject internal constructor(
    internal val bankRepository: BankRepository,
    internal val addressRepository: AddressFieldElementRepository
) {
    private var initialized = false
    internal fun init() {
        if (!initialized) {
            initialized = true
            bankRepository.init()
            addressRepository.init()
        }
    }
}
