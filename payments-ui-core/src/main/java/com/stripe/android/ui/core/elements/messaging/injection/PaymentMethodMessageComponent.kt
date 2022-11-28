package com.stripe.android.ui.core.elements.messaging.injection

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.ui.core.elements.messaging.PaymentMethodMessageViewModel
import com.stripe.android.ui.core.elements.messaging.PaymentMethodMessageView
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        PaymentMethodMessageModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class
    ]
)
internal interface PaymentMethodMessageComponent {
    val viewModel: PaymentMethodMessageViewModel

    fun inject(factory: PaymentMethodMessageViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun configuration(configuration: PaymentMethodMessageView.Configuration): Builder

        fun build(): PaymentMethodMessageComponent
    }
}
