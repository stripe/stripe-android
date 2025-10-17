package com.stripe.android.paymentmethodmessaging.view.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentmethodmessaging.view.messagingelement.MessagingViewModel
import com.stripe.android.paymentmethodmessaging.view.messagingelement.PaymentMethodMessagingElement
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        PaymentMethodMessagingModule::class,
        StripeRepositoryModule::class,
        PaymentElementRequestSurfaceModule::class,
        CoreCommonModule::class
    ]
)
internal interface PaymentMethodMessagingComponent {
    val element: PaymentMethodMessagingElement
    val viewModel: MessagingViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): PaymentMethodMessagingComponent
    }
}

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        PaymentMethodMessagingModule::class,
        StripeRepositoryModule::class,
        PaymentElementRequestSurfaceModule::class,
        CoreCommonModule::class
    ]
)
internal interface PaymentMethodMessagingViewModelComponent {
    val viewModel: MessagingViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance application: Application,
        ): PaymentMethodMessagingViewModelComponent
    }

}
