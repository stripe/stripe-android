package com.stripe.android.hcaptcha.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.hcaptcha.HCaptchaException
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal class DefaultCaptchaEventsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
    private val errorReporter: ErrorReporter
) : CaptchaEventsReporter {

    override fun init(siteKey: String) {
        durationProvider.start(DurationProvider.Key.Captcha)
        fireEvent(CaptchaAnalyticsEvent.Init(siteKey))
    }

    override fun execute(siteKey: String) {
        fireEvent(CaptchaAnalyticsEvent.Execute(siteKey))
    }

    override fun success(siteKey: String) {
        val duration = durationProvider.end(DurationProvider.Key.Captcha)
        fireEvent(
            event = CaptchaAnalyticsEvent.Success(siteKey),
            additionalParams = durationInMsFromStart(duration)
        )
    }

    override fun error(error: Throwable?, siteKey: String) {
        val duration = durationProvider.end(DurationProvider.Key.Captcha)

        when (error) {
            is HCaptchaException -> {
                errorReporter.report(ErrorReporter.ExpectedErrorEvent.HCAPTCHA_FAILURE)
            }
            else -> {
                errorReporter.report(ErrorReporter.UnexpectedErrorEvent.HCAPTCHA_UNEXPECTED_FAILURE)
            }
        }

        fireEvent(
            event = CaptchaAnalyticsEvent.Error(error, siteKey),
            additionalParams = durationInMsFromStart(duration)
        )
    }

    override fun attachStart() {
        durationProvider.start(DurationProvider.Key.CaptchaAttach)
    }

    override fun attachEnd(siteKey: String, isReady: Boolean) {
        val duration = durationProvider.end(DurationProvider.Key.CaptchaAttach)
        fireEvent(
            event = CaptchaAnalyticsEvent.Attach(isReady, siteKey),
            additionalParams = durationInMsFromStart(duration)
        )
    }

    private fun fireEvent(
        event: CaptchaAnalyticsEvent,
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
