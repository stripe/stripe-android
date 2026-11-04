package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.hcaptcha.DefaultHCaptchaService.CachedResult
import com.stripe.android.hcaptcha.analytics.CaptchaEventsReporter
import com.stripe.android.hcaptcha.analytics.DefaultCaptchaEventsReporter
import com.stripe.android.payments.core.analytics.ErrorReporter
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object HCaptchaModule {
    // Share prefetch state across components without retaining their analytics dependencies.
    private val cachedResult = MutableStateFlow<CachedResult>(CachedResult.Idle)

    @Provides
    internal fun provideCachedResult(): MutableStateFlow<CachedResult> = cachedResult

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
        hCaptchaProvider: HCaptchaProvider,
        captchaEventsReporter: CaptchaEventsReporter,
        webViewStarter: WebViewStarter,
        cachedResult: MutableStateFlow<CachedResult>
    ): HCaptchaService {
        return DefaultHCaptchaService(
            hCaptchaProvider = hCaptchaProvider,
            captchaEventsReporter = captchaEventsReporter,
            webViewStarter = webViewStarter,
            cachedResult = cachedResult
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
