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

// TODO(tillh-stripe): Remove this once we no longer confirm in the add-payment-method screen
internal suspend fun LinkAccountManager.loadDefaultShippingAddress(): ConsumerShippingAddress? {
    val shippingAddresses = consumerShippingAddresses.value ?: listShippingAddresses().getOrNull() ?: return null
    val address = shippingAddresses.addresses.firstOrNull { it.isDefault } ?: shippingAddresses.addresses.firstOrNull()
    return address?.copy(unredactedPhoneNumber = linkAccountInfo.value.account?.unredactedPhoneNumber)
}
