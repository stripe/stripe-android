package com.stripe.android.link.account

import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerSession

internal interface MutableLinkAccountManager : LinkAccountManager {
    fun setAccount(
        consumerSession: ConsumerSession?,
        publishableKey: String?,
    ): LinkAccount?
}
