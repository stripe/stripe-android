package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressElementViewModel
import com.stripe.android.paymentsheet.addresselement.AutocompleteViewModel
import com.stripe.android.paymentsheet.addresselement.InputAddressViewModel
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
        LoggingModule::class,
        AddressLauncherResourceRepositoryModule::class,
        AddressElementViewModelModule::class,
        FormControllerModule::class
    ]
)
internal interface AddressElementViewModelFactoryComponent {
    fun inject(factory: AddressElementViewModel.Factory)
    fun inject(factory: InputAddressViewModel.Factory)
    fun inject(factory: AutocompleteViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun starterArgs(starterArgs: AddressElementActivityContract.Args): Builder

        fun build(): AddressElementViewModelFactoryComponent
    }
}
