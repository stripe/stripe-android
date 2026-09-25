package com.stripe.android.link.analytics

internal interface LinkEventsReporter {
    fun onInvalidSessionState(state: SessionState)
    fun onInvalidSessionState(state: SessionState, publishableKey: String) = onInvalidSessionState(state)

    fun onInlineSignupCheckboxChecked()
    fun onSignupFlowPresented()
    fun onSignupStarted(isInline: Boolean = false)
    fun onSignupCompleted(isInline: Boolean = false)
    fun onSignupCompleted(isInline: Boolean = false, publishableKey: String) = onSignupCompleted(isInline)
    fun onSignupFailure(isInline: Boolean = false, error: Throwable)
    fun onSignupFailure(isInline: Boolean = false, error: Throwable, publishableKey: String) =
        onSignupFailure(isInline, error)
    fun onEmailSuggestionAccepted()
    fun onAccountLookupFailure(error: Throwable)
    fun onAccountLookupFailure(error: Throwable, publishableKey: String) = onAccountLookupFailure(error)
    fun onAccountLookupComplete()
    fun onAccountLookupComplete(publishableKey: String) = onAccountLookupComplete()
    fun onAccountRefreshFailure(error: Throwable)
    fun onAccountRefreshFailure(error: Throwable, publishableKey: String) = onAccountRefreshFailure(error)

    fun on2FAStart()
    fun on2FAStart(publishableKey: String) = on2FAStart()
    fun on2FAStartFailure()
    fun on2FAStartFailure(publishableKey: String) = on2FAStartFailure()
    fun on2FAComplete()
    fun on2FAComplete(publishableKey: String) = on2FAComplete()
    fun on2FAFailure()
    fun on2FAFailure(publishableKey: String) = on2FAFailure()
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
