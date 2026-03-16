package com.stripe.android.link.account

internal interface LinkStore {
    fun hasUsedLink(): Boolean
    fun markLinkAsUsed()
    fun clear()
}
