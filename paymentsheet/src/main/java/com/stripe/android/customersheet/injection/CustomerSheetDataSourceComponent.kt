package com.stripe.android.customersheet.injection

import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CustomerSheetDataSourceModule::class,
        CoroutineContextModule::class,
    ]
)
@OptIn(ExperimentalCustomerSheetApi::class)
internal interface CustomerSheetDataSourceComponent {
    val dataSource: CombinedDataSource<*>

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun customerAdapter(adapter: CustomerAdapter): Builder

        fun build(): CustomerSheetDataSourceComponent
    }

    class CombinedDataSource<T>(dataSource: T) :
        CustomerSheetSavedSelectionDataSource by dataSource,
        CustomerSheetPaymentMethodDataSource by dataSource,
        CustomerSheetIntentDataSource by dataSource
        where T : CustomerSheetSavedSelectionDataSource,
              T : CustomerSheetPaymentMethodDataSource,
              T : CustomerSheetIntentDataSource
}
