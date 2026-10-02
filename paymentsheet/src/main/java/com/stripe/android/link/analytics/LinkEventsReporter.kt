package com.stripe.android.link.analytics

internal interface LinkEventsReporter {
    fun onInvalidSessionState(state: SessionState)

    fun onInlineSignupCheckboxChecked()
    fun onSignupFlowPresented()
    fun onSignupStarted(isInline: Boolean = false)
    fun onSignupCompleted(isInline: Boolean = false)
    fun onSignupFailure(isInline: Boolean = false, error: Throwable)
    fun onEmailSuggestionAccepted()
    fun onAccountLookupFailure(error: Throwable)
    fun onAccountLookupComplete()
    fun onAccountRefreshFailure(error: Throwable)

    fun on2FAStart(verificationType: String)
    fun on2FAStartFailure(verificationType: String)
    fun on2FAComplete(verificationType: String)
    fun on2FAFailure(verificationType: String)
    fun on2FACancel()
    fun on2FAResendCode(verificationType: String)

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
