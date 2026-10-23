package com.stripe.android.hcaptcha.analytics

import app.cash.turbine.Turbine

internal class FakeCaptchaEventsReporter : CaptchaEventsReporter {
    val calls = Turbine<Call>()

    override fun init(siteKey: String) {
        calls.add(Call.Init(siteKey))
    }

    override fun execute(siteKey: String) {
        calls.add(Call.Execute(siteKey))
    }

    override fun success(siteKey: String) {
        calls.add(Call.Success(siteKey))
    }

    override fun error(error: Throwable?, siteKey: String) {
        calls.add(Call.Error(error, siteKey))
    }

    override fun attachStart() {
        calls.add(Call.AttachStart)
    }

    override fun attachEnd(siteKey: String, isReady: Boolean) {
        calls.add(Call.AttachEnd(siteKey, isReady))
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    sealed interface Call {
        data class Init(val siteKey: String) : Call
        data class Execute(val siteKey: String) : Call
        data class Success(val siteKey: String) : Call
        data class Error(val error: Throwable?, val siteKey: String) : Call
        data object AttachStart : Call
        data class AttachEnd(val siteKey: String, val isReady: Boolean) : Call
    }
}
