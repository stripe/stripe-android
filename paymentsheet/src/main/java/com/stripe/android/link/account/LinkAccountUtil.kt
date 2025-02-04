package com.stripe.android.link.account

import com.stripe.android.link.LinkAccountUpdate

internal val LinkAccountManager.linkAccountUpdate: LinkAccountUpdate
    get() = LinkAccountUpdate.Value(linkAccount.value)
