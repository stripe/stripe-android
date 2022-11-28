package com.stripe.android.paymentmethodmessage.view.injection

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.paymentmethodmessage.view.PaymentMethodMessageView
import com.stripe.android.paymentmethodmessage.view.PaymentMethodMessageViewModel
import com.stripe.android.payments.core.injection.StripeRepositoryModule
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
