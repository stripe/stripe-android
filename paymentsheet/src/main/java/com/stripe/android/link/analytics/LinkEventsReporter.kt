package com.stripe.android.link.analytics

internal interface LinkEventsReporter {
    fun onInvalidSessionState(state: SessionState)

    fun onInlineSignupCheckboxChecked(checkoutSessionId: String?)
    fun onSignupFlowPresented()
    fun onSignupStarted(isInline: Boolean, checkoutSessionId: String?)
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
    fun on2FAResendCode(verificationType: String, checkoutSessionId: String?)

    fun onPopupShow(checkoutSessionId: String?)
    fun onPopupSuccess(checkoutSessionId: String?)
    fun onPopupCancel(checkoutSessionId: String?)
    fun onPopupError(error: Throwable, checkoutSessionId: String?)
    fun onPopupLogout(checkoutSessionId: String?)
    fun onPopupSkipped(checkoutSessionId: String?)

    enum class SessionState {
        RequiresSignUp, RequiresVerification, Verified
    }
}
