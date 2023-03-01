package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.ConfirmCallback
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        PaymentSheetCommonModule::class,
        PaymentOptionsViewModelModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class
    ]
)
internal abstract class PaymentOptionsViewModelFactoryComponent : NonFallbackInjector {
    abstract fun inject(factory: PaymentOptionsViewModel.Factory)
    abstract fun inject(factory: FormViewModel.Factory)

    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is PaymentOptionsViewModel.Factory -> inject(injectable)
            is FormViewModel.Factory -> inject(injectable)
            else -> {
                throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
            }
        }
    }

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        @BindsInstance
        fun confirmCallback(confirmCallback: ConfirmCallback?): Builder

        fun build(): PaymentOptionsViewModelFactoryComponent
    }
}
