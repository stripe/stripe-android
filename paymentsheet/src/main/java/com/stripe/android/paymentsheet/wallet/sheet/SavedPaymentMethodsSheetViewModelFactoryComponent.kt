package com.stripe.android.paymentsheet.wallet.sheet

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.PaymentSheetCommonModule
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
        SavedPaymentMethodsSheetViewModelModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
        SavedPaymentMethodsSheetAbstractModule::class,
    ]
)
internal abstract class SavedPaymentMethodsSheetViewModelFactoryComponent : NonFallbackInjector {
    abstract fun inject(factory: SavedPaymentMethodsSheetViewModel.Factory)
    abstract fun inject(factory: FormViewModel.Factory)

    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is SavedPaymentMethodsSheetViewModel.Factory -> inject(injectable)
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

        fun build(): SavedPaymentMethodsSheetViewModelFactoryComponent
    }
}
