package com.stripe.android.link.account

import com.stripe.android.link.LinkAccountUpdate

internal val LinkAccountManager.linkAccountUpdate: LinkAccountUpdate
    get() = LinkAccountUpdate.Value(linkAccountInfo.value)

internal fun LinkAccountUpdate.updateLinkAccount(linkAccountHolder: LinkAccountHolder) {
    when (this) {
        is LinkAccountUpdate.Value -> {
            linkAccountHolder.set(linkAccountInfo)
        }
        LinkAccountUpdate.None -> Unit
    }
}
