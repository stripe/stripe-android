package com.stripe.android.link.account

import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.model.ConsumerShippingAddress

internal val LinkAccountManager.linkAccountUpdate: LinkAccountUpdate
    get() = LinkAccountUpdate.Value(linkAccount.value)

internal suspend fun LinkAccountManager.loadDefaultShippingAddress(): ConsumerShippingAddress? {
    val shippingAddresses = cachedShippingAddresses ?: listShippingAddresses().getOrNull() ?: return null
    cachedShippingAddresses = shippingAddresses
    return shippingAddresses.addresses.firstOrNull { it.isDefault } ?: shippingAddresses.addresses.firstOrNull()
}
