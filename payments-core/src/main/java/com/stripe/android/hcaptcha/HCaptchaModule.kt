package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.hcaptcha.analytics.CaptchaEventsReporter
import com.stripe.android.hcaptcha.analytics.DefaultCaptchaEventsReporter
import com.stripe.android.hcaptcha.analytics.DeferringCaptchaEventsReporter
import com.stripe.android.payments.core.analytics.ErrorReporter
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object HCaptchaModule {
    private val deferringCaptchaEventsReporter = DeferringCaptchaEventsReporter()
    private val hCaptchaService = HCaptchaService.create(deferringCaptchaEventsReporter)

    @Provides
    internal fun provideHCaptchaProvider(): HCaptchaProvider {
        return DefaultHCaptchaProvider()
    }

    @Provides
    internal fun provideWebViewStarter(): WebViewStarter {
        return DefaultWebViewStarter
    }

    @Provides
    internal fun provideHCaptchaService(
        captchaEventsReporter: CaptchaEventsReporter,
    ): HCaptchaService {
        return ReportingHCaptchaService(
            service = hCaptchaService,
            deferringCaptchaEventsReporter = deferringCaptchaEventsReporter,
            captchaEventsReporter = captchaEventsReporter,
        )
    }

    @Provides
    internal fun provideChallengeEventsReporter(
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        analyticsRequestFactory: AnalyticsRequestFactory,
        durationProvider: DurationProvider,
        errorReporter: ErrorReporter
    ): CaptchaEventsReporter {
        return DefaultCaptchaEventsReporter(
            analyticsRequestExecutor,
            analyticsRequestFactory,
            durationProvider,
            errorReporter
        )
    }
}
