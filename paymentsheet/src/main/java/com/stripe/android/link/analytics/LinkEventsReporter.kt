package com.stripe.android.link.analytics

internal interface LinkEventsReporter {
    fun onInvalidSessionState(state: SessionState)

    fun onInlineSignupCheckboxChecked()
    fun onSignupFlowPresented()
    fun onSignupStarted(isInline: Boolean = false)
    fun onSignupCompleted(isInline: Boolean = false)
    fun onSignupFailure(isInline: Boolean = false, error: Throwable)
    fun onAccountLookupFailure(error: Throwable)

    fun on2FAStart()
    fun on2FAStartFailure()
    fun on2FAComplete()
    fun on2FAFailure()
    fun on2FACancel()

    fun onPopupShow()
    fun onPopupSuccess()
    fun onPopupCancel()
    fun onPopupError(error: Throwable)
    fun onPopupLogout()
    fun onPopupSkipped()

    enum class SessionState {
        RequiresSignUp, RequiresVerification, Verified
    }
}
