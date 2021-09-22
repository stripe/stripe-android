package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.payments.core.injection.CoroutineContextModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        PaymentSheetCommonModule::class,
        PaymentSheetLauncherModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class
    ]
)
internal interface PaymentSheetLauncherComponent {
    fun inject(factory: PaymentSheetViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): PaymentSheetLauncherComponent
    }
}
