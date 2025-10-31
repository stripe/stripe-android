package com.stripe.android.attestation.analytics

internal interface AttestationAnalyticsEventsReporter {
    fun prepare()

    fun prepareFailed(error: Throwable?)

    fun prepareSucceeded()

    fun requestToken()

    fun requestTokenSucceeded()

    fun requestTokenFailed(error: Throwable?)
}
