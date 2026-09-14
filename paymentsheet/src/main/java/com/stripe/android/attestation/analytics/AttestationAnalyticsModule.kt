package com.stripe.android.attestation.analytics

import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.link.injection.PaymentsIntegrityModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.Module
import dagger.Provides
import javax.inject.Named

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
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
    ): AttestationAnalyticsEventsReporter {
        return DefaultAttestationAnalyticsEventsReporter(
            analyticsRequestExecutor,
            analyticsRequestFactory,
            durationProvider,
            publishableKeyProvider = publishableKeyProvider,
        )
    }
}
