package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressElementViewModel
import com.stripe.android.paymentsheet.addresselement.AutocompleteViewModel
import com.stripe.android.paymentsheet.addresselement.InputAddressViewModel
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import com.stripe.android.ui.core.injection.FormControllerModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        PaymentSheetCommonModule::class,
        CoroutineContextModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class,
        AddressElementViewModelModule::class,
        FormControllerModule::class,
        ResourceRepositoryModule::class
    ]
)
internal abstract class AddressElementViewModelFactoryComponent : NonFallbackInjector {
    abstract fun inject(factory: AddressElementViewModel.Factory)
    abstract fun inject(factory: InputAddressViewModel.Factory)
    abstract fun inject(factory: AutocompleteViewModel.Factory)

    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is AddressElementViewModel.Factory -> inject(injectable)
            is InputAddressViewModel.Factory -> inject(injectable)
            is AutocompleteViewModel.Factory -> inject(injectable)
            else -> {
                throw IllegalArgumentException(
                    "invalid Injectable $injectable requested in $this"
                )
            }
        }
    }

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun starterArgs(starterArgs: AddressElementActivityContract.Args): Builder

        fun build(): AddressElementViewModelFactoryComponent
    }
}
