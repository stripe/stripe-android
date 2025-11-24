package com.stripe.android.challenge.confirmation.analytics

internal interface IntentConfirmationChallengeAnalyticsEventReporter {
    fun start()

    fun success()

    fun error(
        error: Throwable?,
        errorType: String?,
        errorCode: String?,
        fromBridge: Boolean
    )

    fun webViewLoaded()
}
