package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
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
internal interface PaymentOptionsViewModelFactoryComponent {
    val paymentOptionsViewModelSubcomponentBuilder: PaymentOptionsViewModelSubcomponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        fun build(): PaymentOptionsViewModelFactoryComponent
    }
}
