package com.stripe.android.customersheet.data.injection

import com.stripe.android.customersheet.data.CustomerAdapterDataSource
import com.stripe.android.customersheet.data.CustomerSheetInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import dagger.Binds
import dagger.Module

@Module
internal interface CustomerAdapterDataSourceModule {
    @Binds
    fun bindsCustomerSheetPaymentMethodDataSource(
        impl: CustomerAdapterDataSource
    ): CustomerSheetPaymentMethodDataSource

    @Binds
    fun bindsCustomerSheetIntentDataSource(
        impl: CustomerAdapterDataSource
    ): CustomerSheetIntentDataSource

    @Binds
    fun bindsCustomerSheetSavedSelectionDataSource(
        impl: CustomerAdapterDataSource
    ): CustomerSheetSavedSelectionDataSource

    @Binds
    fun bindsCustomerSheetInitializationDataSource(
        impl: CustomerAdapterDataSource
    ): CustomerSheetInitializationDataSource
}
