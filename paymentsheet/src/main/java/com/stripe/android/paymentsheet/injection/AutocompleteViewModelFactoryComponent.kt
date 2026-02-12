package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.core.injection.ApplicationContextModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.addresselement.AutocompleteContract
import com.stripe.android.paymentsheet.addresselement.AutocompleteViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ApplicationContextModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        StripeRepositoryModule::class,
        AutocompleteViewModelModule::class,
    ]
)
internal interface AutocompleteViewModelFactoryComponent {
    val autocompleteViewModel: AutocompleteViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance application: Application,
            @BindsInstance args: AutocompleteContract.Args,
        ): AutocompleteViewModelFactoryComponent
    }
}
