package com.stripe.android.challenge.confirmation

import com.stripe.android.challenge.confirmation.analytics.IntentConfirmationChallengeAnalyticsEventReporter

internal class FakeIntentConfirmationChallengeAnalyticsEventReporter :
    IntentConfirmationChallengeAnalyticsEventReporter {

    private val _calls = mutableListOf<Call>()
    val calls: List<Call> get() = _calls

    override fun onStart(captchaVendorName: String?) {
        _calls.add(Call.Start(captchaVendorName))
    }

    override fun onSuccess(captchaVendorName: String?) {
        _calls.add(Call.Success(captchaVendorName))
    }

    override fun onError(
        errorType: String?,
        errorCode: String?,
        fromBridge: Boolean,
        captchaVendorName: String?
    ) {
        _calls.add(
            Call.Error(
                errorType = errorType,
                errorCode = errorCode,
                fromBridge = fromBridge,
                captchaVendorName = captchaVendorName
            )
        )
    }

    override fun onCancel(captchaVendorName: String?) {
        _calls.add(Call.Cancel(captchaVendorName))
    }

    override fun onWebViewLoaded(captchaVendorName: String?) {
        _calls.add(Call.WebViewLoaded(captchaVendorName))
    }

    fun clear() {
        _calls.clear()
    }

    sealed interface Call {
        data class Start(val captchaVendorName: String?) : Call
        data class Success(val captchaVendorName: String?) : Call
        data class Error(
            val errorType: String?,
            val errorCode: String?,
            val fromBridge: Boolean,
            val captchaVendorName: String?
        ) : Call
        data class Cancel(val captchaVendorName: String?) : Call
        data class WebViewLoaded(val captchaVendorName: String?) : Call
    }
}
