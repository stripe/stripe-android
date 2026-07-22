package com.stripe.android.attestation

import android.app.Application
import com.stripe.android.ApiConfiguration
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.link.injection.PaymentsIntegrityModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.injection.NamedKeysFromApiConfigModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AttestationModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        PaymentsIntegrityModule::class,
        NamedKeysFromApiConfigModule::class,
    ]
)
internal interface AttestationComponent {
    val attestationViewModel: AttestationViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance application: Application,
            @BindsInstance
            apiConfiguration: ApiConfiguration.State,
            @BindsInstance
            @Named(PRODUCT_USAGE)
            productUsage: Set<String>,
        ): AttestationComponent
    }
}
