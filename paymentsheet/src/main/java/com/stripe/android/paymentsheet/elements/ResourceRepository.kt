package com.stripe.android.paymentsheet.elements

import android.content.res.Resources
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository

/**
 * This holds all the resources read in from JSON.
 */
internal class ResourceRepository(
    internal val bankRepository: BankRepository,
    internal val addressRepository: AddressFieldElementRepository
) {
    internal constructor(resources: Resources) : this(
        BankRepository(resources),
        AddressFieldElementRepository(resources)
    )

    internal fun init() {
        bankRepository.init()
        addressRepository.init()
    }

    companion object : SingletonHolder<ResourceRepository, Resources>(::ResourceRepository)
}
