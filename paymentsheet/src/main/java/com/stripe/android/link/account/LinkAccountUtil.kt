package com.stripe.android.link.account

import com.stripe.android.link.LinkAccountUpdate

internal val LinkAccountManager.linkAccountUpdate: LinkAccountUpdate
    get() = linkAccountInfo.value

internal fun LinkAccountUpdate.updateLinkAccount(linkAccountHolder: LinkAccountHolder) {
    when (this) {
        is LinkAccountUpdate.Value -> linkAccountHolder.set(this)
        LinkAccountUpdate.None -> Unit
    }
}
