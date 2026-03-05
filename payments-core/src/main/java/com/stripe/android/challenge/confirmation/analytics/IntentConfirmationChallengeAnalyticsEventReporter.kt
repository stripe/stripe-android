package com.stripe.android.challenge.confirmation.analytics

internal interface IntentConfirmationChallengeAnalyticsEventReporter {
    fun onStart(captchaVendorName: String)

    fun onSuccess(captchaVendorName: String)

    fun onError(
        errorType: String?,
        errorCode: String?,
        fromBridge: Boolean,
        captchaVendorName: String
    )

    fun onCancel(captchaVendorName: String)

    fun onWebViewLoaded(captchaVendorName: String)
}
