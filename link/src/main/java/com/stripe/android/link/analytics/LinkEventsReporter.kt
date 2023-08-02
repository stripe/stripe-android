package com.stripe.android.link.analytics

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface LinkEventsReporter {
    fun onInlineSignupCheckboxChecked()
    fun onSignupStarted(isInline: Boolean = false)
    fun onSignupCompleted(isInline: Boolean = false)
    fun onSignupFailure(isInline: Boolean = false)
    fun onAccountLookupFailure()

    fun onPopupShow()
    fun onPopupSuccess()
    fun onPopupCancel()
    fun onPopupError(error: Throwable)
    fun onPopupLogout()
    fun onPopupSkipped()
}
