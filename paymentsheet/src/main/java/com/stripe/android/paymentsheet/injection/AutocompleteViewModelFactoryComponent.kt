package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.ApiConfiguration
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.payments.core.injection.ApiRequestOptionsModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.addresselement.AutocompleteContract
import com.stripe.android.paymentsheet.addresselement.AutocompleteViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoreCommonModule::class,
        CoroutineContextModule::class,
        StripeRepositoryModule::class,
        ApiRequestOptionsModule::class,
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
            @BindsInstance apiConfigurationState: ApiConfiguration.State,
        ): AutocompleteViewModelFactoryComponent
    }
}
