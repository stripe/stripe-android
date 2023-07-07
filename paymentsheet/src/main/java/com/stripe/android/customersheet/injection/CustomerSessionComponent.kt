package com.stripe.android.customersheet.injection

import android.app.Application
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSessionViewModel
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import dagger.BindsInstance
import dagger.Component

@OptIn(ExperimentalCustomerSheetApi::class)
@CustomerSessionScope
@Component(
    modules = [
        CustomerSheetModule::class,
        CustomerSheetViewModelModule::class,
        StripeRepositoryModule::class,
    ]
)
internal interface CustomerSessionComponent {
    val customerSheetComponentBuilder: CustomerSheetComponent.Builder
    val customerSheetViewModelComponentBuilder: CustomerSheetViewModelComponent.Builder

    val viewModel: CustomerSessionViewModel
    val paymentOptionFactory: PaymentOptionFactory
    val configuration: CustomerSheet.Configuration
    val customerAdapter: CustomerAdapter

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

        fun build(): CustomerSessionComponent
    }
}
