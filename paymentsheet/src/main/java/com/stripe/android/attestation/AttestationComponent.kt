package com.stripe.android.attestation

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.link.injection.PaymentsIntegrityModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        PaymentsIntegrityModule::class
    ]
)
internal interface AttestationComponent {
    val attestationViewModel: AttestationViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun publishableKeyProvider(
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String
        ): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        fun build(): AttestationComponent
    }
}
