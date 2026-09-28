package com.stripe.android.utils

import app.cash.turbine.Turbine
import com.stripe.android.link.account.LinkStore

internal class FakeLinkStore(
    hasUsedLink: Boolean = false,
) : LinkStore {

    private var _hasUsedLink: Boolean = hasUsedLink

    val markAsUsedCalls = Turbine<Unit>()

    override fun hasUsedLink(): Boolean {
        return _hasUsedLink
    }

    override fun markLinkAsUsed() {
        _hasUsedLink = true
        markAsUsedCalls.add(Unit)
    }

    override fun clear() {
        _hasUsedLink = false
    }

    fun ensureAllEventsConsumed() {
        markAsUsedCalls.ensureAllEventsConsumed()
    }
}
