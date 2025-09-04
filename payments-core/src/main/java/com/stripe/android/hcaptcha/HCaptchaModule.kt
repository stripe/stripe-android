package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.hcaptcha.analytics.CaptchaEventsReporter
import com.stripe.android.hcaptcha.analytics.DefaultCaptchaEventsReporter
import com.stripe.android.payments.core.analytics.ErrorReporter
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object HCaptchaModule {
    @Volatile
    private var hCaptchaService: HCaptchaService? = null

    @Provides
    internal fun provideHCaptchaProvider(): HCaptchaProvider {
        return DefaultHCaptchaProvider()
    }

    @Provides
    fun provideHCaptchaService(
        hCaptchaProvider: HCaptchaProvider,
        captchaEventsReporter: CaptchaEventsReporter,
        durationProvider: DurationProvider
    ): HCaptchaService {
        return hCaptchaService ?: synchronized(this) {
            hCaptchaService ?: DefaultHCaptchaService(hCaptchaProvider, captchaEventsReporter, durationProvider)
                .also { hCaptchaService = it }
        }
    }

    @Provides
    internal fun provideChallengeEventsReporter(
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        analyticsRequestFactory: AnalyticsRequestFactory,
        errorReporter: ErrorReporter
    ): CaptchaEventsReporter {
        return DefaultCaptchaEventsReporter(
            analyticsRequestExecutor,
            analyticsRequestFactory,
            errorReporter
        )
    }
}
