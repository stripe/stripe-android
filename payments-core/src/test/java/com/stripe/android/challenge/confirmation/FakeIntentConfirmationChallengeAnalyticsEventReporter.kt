package com.stripe.android.challenge.confirmation

import com.stripe.android.challenge.confirmation.analytics.IntentConfirmationChallengeAnalyticsEventReporter

internal class FakeIntentConfirmationChallengeAnalyticsEventReporter :
    IntentConfirmationChallengeAnalyticsEventReporter {

    private val _calls = mutableListOf<Call>()
    val calls: List<Call> get() = _calls

    override fun onStart() {
        _calls.add(Call.Start)
    }

    override fun onSuccess() {
        _calls.add(Call.Success)
    }

    override fun onError(
        errorType: String?,
        errorCode: String?,
        fromBridge: Boolean
    ) {
        _calls.add(
            Call.Error(
                errorType = errorType,
                errorCode = errorCode,
                fromBridge = fromBridge
            )
        )
    }

    override fun onWebViewLoaded() {
        _calls.add(Call.WebViewLoaded)
    }

    fun clear() {
        _calls.clear()
    }

    sealed interface Call {
        data object Start : Call
        data object Success : Call
        data class Error(
            val errorType: String?,
            val errorCode: String?,
            val fromBridge: Boolean
        ) : Call
        data object WebViewLoaded : Call
    }
}
