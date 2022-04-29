package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
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
        CoroutineContextModule::class,
        LoggingModule::class,
        ResourceRepositoryModule::class
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
        fun injectorKey(@InjectorKey injectorKey: String): Builder

        fun build(): PaymentSheetLauncherComponent
    }
}
