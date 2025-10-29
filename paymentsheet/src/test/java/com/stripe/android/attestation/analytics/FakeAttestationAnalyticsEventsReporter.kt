package com.stripe.android.attestation.analytics

import app.cash.turbine.Turbine

internal class FakeAttestationAnalyticsEventsReporter : AttestationAnalyticsEventsReporter {
    private val calls = Turbine<Call>()

    override fun prepare() {
        calls.add(Call.Prepare)
    }

    override fun prepareFailed(error: Throwable?) {
        calls.add(Call.PrepareFailed(error))
    }

    override fun prepareSucceeded() {
        calls.add(Call.PrepareSucceeded)
    }

    override fun requestToken() {
        calls.add(Call.RequestToken)
    }

    override fun requestTokenSucceeded() {
        calls.add(Call.RequestTokenSucceeded)
    }

    override fun requestTokenFailed(error: Throwable?) {
        calls.add(Call.RequestTokenFailed(error))
    }

    suspend fun awaitCall(): Call = calls.awaitItem()

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    sealed interface Call {
        data object Prepare : Call
        data class PrepareFailed(val error: Throwable?) : Call
        data object PrepareSucceeded : Call
        data object RequestToken : Call
        data object RequestTokenSucceeded : Call
        data class RequestTokenFailed(val error: Throwable?) : Call
    }
}