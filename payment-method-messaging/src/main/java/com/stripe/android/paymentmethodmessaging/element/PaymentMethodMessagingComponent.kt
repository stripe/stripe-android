package com.stripe.android.paymentmethodmessaging.element

import android.app.Application
import com.stripe.android.core.injection.ApplicationContextModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        ApplicationContextModule::class,
        PaymentMethodMessagingModule::class,
        StripeRepositoryModule::class,
        PaymentElementRequestSurfaceModule::class,
        CoreCommonModule::class
    ]
)
internal interface PaymentMethodMessagingComponent {
    @OptIn(PaymentMethodMessagingElementPreview::class)
    val element: PaymentMethodMessagingElement

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            application: Application,
        ): PaymentMethodMessagingComponent
    }
}
