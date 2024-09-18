package com.stripe.android.customersheet.injection

import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.data.CustomerAdapterDataSource
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCustomerSheetApi::class)
@Module
internal object CustomerSheetDataSourceModule {
    @Provides
    fun providesCombinedDataSource(
        adapter: CustomerAdapter,
        @IOContext workContext: CoroutineContext
    ): CustomerSheetDataSourceComponent.CombinedDataSource<*> {
        return CustomerSheetDataSourceComponent.CombinedDataSource(
            CustomerAdapterDataSource(adapter, workContext)
        )
    }
}
