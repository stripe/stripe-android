package com.stripe.android.attestation.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.injection.PaymentsIntegrityModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@AttestationAnalyticsScope
@Module(
    includes = [
        StripeRepositoryModule::class,
        PaymentsIntegrityModule::class
    ],
)
internal object AttestationAnalyticsModule {

    @Provides
    internal fun provideAttestationAnalyticsEventsReporter(
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        analyticsRequestFactory: AnalyticsRequestFactory,
        durationProvider: DurationProvider,
    ): AttestationAnalyticsEventsReporter {
        return DefaultAttestationAnalyticsEventsReporter(
            analyticsRequestExecutor,
            analyticsRequestFactory,
            durationProvider,
        )
    }
}
