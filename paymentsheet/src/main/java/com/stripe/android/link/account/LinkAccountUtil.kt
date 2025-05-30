package com.stripe.android.link.account

import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.model.ConsumerShippingAddress

internal val LinkAccountManager.linkAccountUpdate: LinkAccountUpdate
    get() = linkAccountInfo.value

internal fun LinkAccountUpdate.updateLinkAccount(linkAccountHolder: LinkAccountHolder) {
    when (this) {
        is LinkAccountUpdate.Value -> linkAccountHolder.set(this)
        LinkAccountUpdate.None -> Unit
    }
}

internal suspend fun LinkAccountManager.loadDefaultShippingAddress(): ConsumerShippingAddress? {
    val shippingAddresses = cachedShippingAddresses ?: listShippingAddresses().getOrNull() ?: return null
    cachedShippingAddresses = shippingAddresses
    val address = shippingAddresses.addresses.firstOrNull { it.isDefault } ?: shippingAddresses.addresses.firstOrNull()
    return address?.copy(unredactedPhoneNumber = linkAccountInfo.value.account?.unredactedPhoneNumber)
}
