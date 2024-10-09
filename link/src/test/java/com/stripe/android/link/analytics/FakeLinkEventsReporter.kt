package com.stripe.android.link.analytics

internal open class FakeLinkEventsReporter : LinkEventsReporter {
    var calledCount = 0
    override fun onInvalidSessionState(state: LinkEventsReporter.SessionState) {
        throw NotImplementedError()
    }

    override fun onInlineSignupCheckboxChecked() {
        throw NotImplementedError()
    }

    override fun onSignupFlowPresented() {
        throw NotImplementedError()
    }

    override fun onSignupStarted(isInline: Boolean) {
        throw NotImplementedError()
    }

    override fun onSignupCompleted(isInline: Boolean) {
        throw NotImplementedError()
    }

    override fun onSignupFailure(isInline: Boolean, error: Throwable) {
        throw NotImplementedError()
    }

    override fun onAccountLookupFailure(error: Throwable) {
        throw NotImplementedError()
    }

    override fun onPopupShow() {
        throw NotImplementedError()
    }

    override fun onPopupSuccess() {
        throw NotImplementedError()
    }

    override fun onPopupCancel() {
        throw NotImplementedError()
    }

    override fun onPopupError(error: Throwable) {
        throw NotImplementedError()
    }

    override fun onPopupLogout() {
        throw NotImplementedError()
    }

    override fun onPopupSkipped() {
        throw NotImplementedError()
    }
}
