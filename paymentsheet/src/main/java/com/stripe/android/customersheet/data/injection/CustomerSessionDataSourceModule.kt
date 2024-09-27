package com.stripe.android.customersheet.data.injection

import com.stripe.android.customersheet.data.CustomerSessionInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSessionIntentDataSource
import com.stripe.android.customersheet.data.CustomerSessionPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSessionSavedSelectionDataSource
import com.stripe.android.customersheet.data.CustomerSheetInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import dagger.Binds
import dagger.Module

@Module
internal interface CustomerSessionDataSourceModule {
    @Binds
    fun bindsCustomerSheetPaymentMethodDataSource(
        impl: CustomerSessionPaymentMethodDataSource
    ): CustomerSheetPaymentMethodDataSource

    @Binds
    fun bindsCustomerSheetIntentDataSource(
        impl: CustomerSessionIntentDataSource
    ): CustomerSheetIntentDataSource

    @Binds
    fun bindsCustomerSheetSavedSelectionDataSource(
        impl: CustomerSessionSavedSelectionDataSource
    ): CustomerSheetSavedSelectionDataSource

    @Binds
    fun bindsCustomerSheetInitializationDataSource(
        impl: CustomerSessionInitializationDataSource
    ): CustomerSheetInitializationDataSource
}
