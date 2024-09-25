package com.stripe.android.customersheet.data.injection

import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@OptIn(ExperimentalCustomerSheetApi::class)
@Singleton
@Component(
    modules = [
        CustomerAdapterDataSourceModule::class
    ]
)
internal interface CustomerAdapterDataSourceComponent {
    val customerSheetPaymentMethodDataSource: CustomerSheetPaymentMethodDataSource
    val customerSheetSavedSelectionDataSource: CustomerSheetSavedSelectionDataSource
    val customerSheetIntentDataSource: CustomerSheetIntentDataSource

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun adapter(customerAdapter: CustomerAdapter): Builder

        fun build(): CustomerAdapterDataSourceComponent
    }
}
