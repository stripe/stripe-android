package com.stripe.android.link.analytics

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface LinkEventsReporter {
    fun onInlineSignupCheckboxChecked()
    fun onSignupFlowPresented()
    fun onSignupStarted(isInline: Boolean = false)
    fun onSignupCompleted(isInline: Boolean = false)
    fun onSignupFailure(isInline: Boolean = false)
    fun onAccountLookupFailure()

    fun on2FAStart()
    fun on2FAStartFailure()
    fun on2FAComplete()
    fun on2FAFailure()
    fun on2FACancel()
}
