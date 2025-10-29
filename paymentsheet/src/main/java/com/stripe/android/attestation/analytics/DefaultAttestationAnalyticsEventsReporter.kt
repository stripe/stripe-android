package com.stripe.android.attestation.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal class DefaultAttestationAnalyticsEventsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
) : AttestationAnalyticsEventsReporter {

    override fun prepare() {
        durationProvider.start(DurationProvider.Key.PrepareAttestation)
        fireEvent(AttestationAnalyticsEvent.Prepare)
    }

    override fun prepareFailed(error: Throwable?) {
        val duration = durationProvider.end(DurationProvider.Key.PrepareAttestation)
        fireEvent(
            event = AttestationAnalyticsEvent.PrepareFailed(error),
            additionalParams = durationInMsFromStart(duration)
        )
    }

    override fun prepareSucceeded() {
        val duration = durationProvider.end(DurationProvider.Key.PrepareAttestation)
        fireEvent(
            event = AttestationAnalyticsEvent.PrepareSucceeded,
            additionalParams = durationInMsFromStart(duration)
        )
    }

    override fun requestToken() {
        durationProvider.start(DurationProvider.Key.Attest)
        fireEvent(AttestationAnalyticsEvent.RequestToken)
    }

    override fun requestTokenSucceeded() {
        val duration = durationProvider.end(DurationProvider.Key.Attest)
        fireEvent(
            event = AttestationAnalyticsEvent.RequestTokenSucceeded,
            additionalParams = durationInMsFromStart(duration)
        )
    }

    override fun requestTokenFailed(error: Throwable?) {
        val duration = durationProvider.end(DurationProvider.Key.Attest)
        fireEvent(
            event = AttestationAnalyticsEvent.RequestTokenFailed(error),
            additionalParams = durationInMsFromStart(duration)
        )
    }

    private fun fireEvent(
        event: AttestationAnalyticsEvent,
        additionalParams: Map<String, Any> = emptyMap()
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = event,
                additionalParams = event.params + additionalParams
            )
        )
    }

    private fun durationInMsFromStart(duration: Duration?): Map<String, Float> {
        return duration?.let {
            mapOf("duration" to it.toDouble(DurationUnit.MILLISECONDS).toFloat())
        } ?: emptyMap()
    }
}
