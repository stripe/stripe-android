package com.stripe.android.attestation.analytics

import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal class DefaultAttestationAnalyticsEventsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
) : AttestationAnalyticsEventsReporter {

    override fun prepare() {
        durationProvider.start(DurationProvider.Key.PrepareAttestation)
        fireEvent(AttestationAnalyticsEvent.Prepare)
    }

    override fun prepareFailed(error: Throwable?) {
        val duration = durationProvider.end(DurationProvider.Key.PrepareAttestation)
        fireEvent(
            event = AttestationAnalyticsEvent.PrepareFailed(
                error,
                duration = durationInMs(duration)
            )
        )
    }

    override fun prepareSucceeded() {
        val duration = durationProvider.end(DurationProvider.Key.PrepareAttestation)
        fireEvent(AttestationAnalyticsEvent.PrepareSucceeded(durationInMs(duration)))
    }

    override fun requestToken() {
        durationProvider.start(DurationProvider.Key.Attest)
        fireEvent(AttestationAnalyticsEvent.RequestToken)
    }

    override fun requestTokenSucceeded() {
        val duration = durationProvider.end(DurationProvider.Key.Attest)
        fireEvent(AttestationAnalyticsEvent.RequestTokenSucceeded(durationInMs(duration)))
    }

    override fun requestTokenFailed(error: Throwable?) {
        val duration = durationProvider.end(DurationProvider.Key.Attest)
        fireEvent(
            event = AttestationAnalyticsEvent.RequestTokenFailed(
                error,
                duration = durationInMs(duration)
            )
        )
    }

    private fun fireEvent(event: AttestationAnalyticsEvent,) {
        val publishableKey = runCatching { publishableKeyProvider() }.getOrNull()
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = event,
                additionalParams = event.params,
                publishableKey = publishableKey,
            )
        )
    }

    private fun durationInMs(duration: Duration?) = duration?.toDouble(DurationUnit.MILLISECONDS)?.toFloat()
}
