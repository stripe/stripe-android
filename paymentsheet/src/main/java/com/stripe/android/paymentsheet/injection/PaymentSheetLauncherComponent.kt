package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.googlepaylauncher.GooglePayLauncherModule
import com.stripe.android.payments.core.injection.CoroutineContextModule
import com.stripe.android.payments.core.injection.InjectorKey
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.forms.FormViewModel
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
    fun inject(factory: FormViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun injectorKey(@InjectorKey injectorKey: Int): Builder

        fun build(): PaymentSheetLauncherComponent
    }
}
