package com.stripe.android.challenge.confirmation.analytics

internal interface IntentConfirmationChallengeAnalyticsEventReporter {
    fun onStart()

    fun onSuccess()

    fun onError(
        errorType: String?,
        errorCode: String?,
        fromBridge: Boolean
    )

    fun onWebViewLoaded()
}
