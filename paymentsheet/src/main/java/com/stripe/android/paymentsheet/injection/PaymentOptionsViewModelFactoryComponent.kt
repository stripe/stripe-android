package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.payments.core.injection.CoroutineContextModule
import com.stripe.android.payments.core.injection.LoggingModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
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
        LoggingModule::class
    ]
)
internal interface PaymentOptionsViewModelFactoryComponent {
    fun inject(factory: PaymentOptionsViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        fun build(): PaymentOptionsViewModelFactoryComponent
    }
}
