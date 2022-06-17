package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.addresscollection.AddressOptionsActivityContract
import com.stripe.android.paymentsheet.addresscollection.AddressOptionsViewModel
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import com.stripe.android.ui.core.injection.FormControllerModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        PaymentSheetCommonModule::class,
        PaymentOptionsViewModelModule::class,
        CoroutineContextModule::class,
        StripeRepositoryModule::class,
        LoggingModule::class,
        ResourceRepositoryModule::class,
        AddressOptionsViewModelModule::class,
        FormControllerModule::class
    ]
)
internal interface AddressOptionsViewModelFactoryComponent {
    fun inject(factory: AddressOptionsViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun starterArgs(starterArgs: AddressOptionsActivityContract.Args): Builder

        @BindsInstance
        fun enableLogging(@Named(ENABLE_LOGGING) enableLogging: Boolean): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        fun build(): AddressOptionsViewModelFactoryComponent
    }
}