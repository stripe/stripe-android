package com.stripe.android.customersheet.injection

import android.app.Application
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSessionViewModel
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResultCallback
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component

@OptIn(ExperimentalCustomerSheetApi::class)
@CustomerSessionScope
@Component(
    modules = [
        CustomerSheetViewModelModule::class,
        StripeRepositoryModule::class,
        GooglePayLauncherModule::class,
    ]
)
internal interface CustomerSessionComponent {
    val customerSheetComponentBuilder: CustomerSheetComponent.Builder
    val customerSheetViewModelComponentBuilder: CustomerSheetViewModelComponent.Builder

    val configuration: CustomerSheet.Configuration
    val customerAdapter: CustomerAdapter
    val callback: CustomerSheetResultCallback

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun customerSessionViewModel(viewModel: CustomerSessionViewModel): Builder

        @BindsInstance
        fun configuration(configuration: CustomerSheet.Configuration): Builder

        @BindsInstance
        fun customerAdapter(customerAdapter: CustomerAdapter): Builder

        @BindsInstance
        fun callback(callback: CustomerSheetResultCallback): Builder

        @BindsInstance
        fun statusBarColor(statusBarColor: () -> Int?): Builder

        fun build(): CustomerSessionComponent
    }
}
