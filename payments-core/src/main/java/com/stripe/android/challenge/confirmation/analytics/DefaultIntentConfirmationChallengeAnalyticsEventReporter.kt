package com.stripe.android.challenge.confirmation.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal class DefaultIntentConfirmationChallengeAnalyticsEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
) : IntentConfirmationChallengeAnalyticsEventReporter {

    override fun start() {
        durationProvider.start(DurationProvider.Key.IntentConfirmationChallenge)
        durationProvider.start(DurationProvider.Key.IntentConfirmationChallengeWebViewLoaded)
        fireEvent(IntentConfirmationChallengeAnalyticsEvent.Start())
    }

    override fun success() {
        val duration = durationProvider.end(DurationProvider.Key.IntentConfirmationChallenge)
        fireEvent(IntentConfirmationChallengeAnalyticsEvent.Success(durationInMs(duration)))
    }

    override fun error(
        error: Throwable?,
        errorType: String?,
        errorCode: String?,
        fromBridge: Boolean
    ) {
        val duration = durationProvider.end(DurationProvider.Key.IntentConfirmationChallenge)
        fireEvent(
            event = IntentConfirmationChallengeAnalyticsEvent.Error(
                duration = durationInMs(duration),
                errorType = errorType,
                errorCode = errorCode,
                fromBridge = fromBridge,
                error = error
            )
        )
    }

    override fun webViewLoaded() {
        val duration = durationProvider.end(DurationProvider.Key.IntentConfirmationChallengeWebViewLoaded)
        fireEvent(IntentConfirmationChallengeAnalyticsEvent.WebViewLoaded(durationInMs(duration)))
    }

    private fun fireEvent(event: IntentConfirmationChallengeAnalyticsEvent) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = event,
                additionalParams = event.params
            )
        )
    }

    private fun durationInMs(duration: Duration?) = duration?.toDouble(DurationUnit.MILLISECONDS)?.toFloat() ?: 0f
}
