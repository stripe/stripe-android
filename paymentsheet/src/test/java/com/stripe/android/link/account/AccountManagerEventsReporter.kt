package com.stripe.android.link.account

import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter

internal open class AccountManagerEventsReporter : FakeLinkEventsReporter() {
    override fun onInvalidSessionState(state: LinkEventsReporter.SessionState) = Unit
    override fun onSignupCompleted(isInline: Boolean) = Unit
    override fun onSignupFailure(isInline: Boolean, error: Throwable) = Unit
    override fun onAccountLookupFailure(error: Throwable) = Unit
    override fun on2FAStartFailure() = Unit
    override fun on2FAStart() = Unit
    override fun on2FAComplete() = Unit
    override fun on2FAFailure() = Unit
}