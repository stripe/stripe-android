package com.stripe.android.customersheet.injection

import android.content.Context
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSessionViewModel
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResultCallback
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import dagger.BindsInstance
import dagger.Component

@OptIn(ExperimentalCustomerSheetApi::class)
@Component
internal interface CustomerSessionComponent {
    val customerSheetComponentBuilder: CustomerSheetComponent.Builder

    val configuration: CustomerSheet.Configuration
    val customerAdapter: CustomerAdapter
    val callback: CustomerSheetResultCallback

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun appContext(appContext: Context): Builder

        @BindsInstance
        fun customerSessionViewModel(viewModel: CustomerSessionViewModel): Builder

        @BindsInstance
        fun configuration(configuration: CustomerSheet.Configuration): Builder

        @BindsInstance
        fun customerAdapter(customerAdapter: CustomerAdapter): Builder

        @BindsInstance
        fun callback(callback: CustomerSheetResultCallback): Builder

        fun build(): CustomerSessionComponent
    }
}
