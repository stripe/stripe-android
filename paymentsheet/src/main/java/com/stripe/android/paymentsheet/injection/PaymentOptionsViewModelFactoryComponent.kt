package com.stripe.android.paymentsheet.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        PaymentSheetCommonModule::class,
        PaymentElementRequestSurfaceModule::class,
        PaymentOptionsViewModelModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
    ]
)
internal interface PaymentOptionsViewModelFactoryComponent {
    val viewModel: PaymentOptionsViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance
            application: Application,
            @BindsInstance
            handle: SavedStateHandle,
            @BindsInstance
            args: PaymentOptionContract.Args,
        ): PaymentOptionsViewModelFactoryComponent
    }
}
