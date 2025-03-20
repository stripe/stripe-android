package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.link.injection.LinkExtrasModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressElementViewModel
import com.stripe.android.paymentsheet.addresselement.FormControllerModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
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
        ResourceRepositoryModule::class,
        LinkExtrasModule::class
    ]
)
internal interface AddressElementViewModelFactoryComponent {
    val addressElementViewModel: AddressElementViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun starterArgs(starterArgs: AddressElementActivityContract.Args): Builder

        fun build(): AddressElementViewModelFactoryComponent
    }
}
