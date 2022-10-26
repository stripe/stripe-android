package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
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
        CoreCommonModule::class,
        ResourceRepositoryModule::class
    ]
)
internal abstract class PaymentSheetLauncherComponent : NonFallbackInjector {
    abstract fun inject(factory: PaymentSheetViewModel.Factory)
    abstract fun inject(factory: FormViewModel.Factory)

    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is PaymentSheetViewModel.Factory -> inject(injectable)
            is FormViewModel.Factory -> inject(injectable)
            else -> {
                throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
            }
        }
    }

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun injectorKey(@InjectorKey injectorKey: String): Builder

        fun build(): PaymentSheetLauncherComponent
    }
}
