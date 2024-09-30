package com.stripe.android.customersheet.data.injection

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.data.CustomerSessionElementsSessionManager
import com.stripe.android.customersheet.data.CustomerSessionInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSessionIntentDataSource
import com.stripe.android.customersheet.data.CustomerSessionPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSessionSavedSelectionDataSource
import com.stripe.android.customersheet.data.CustomerSheetInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import com.stripe.android.customersheet.data.DefaultCustomerSessionElementsSessionManager
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

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

    @Binds
    fun bindsCustomerSessionElementsSessionManager(
        impl: DefaultCustomerSessionElementsSessionManager
    ): CustomerSessionElementsSessionManager

    companion object {
        @Provides
        fun providePrefsRepositoryFactory(
            appContext: Context,
            @IOContext workContext: CoroutineContext
        ): (String) -> PrefsRepository = { customerId ->
            DefaultPrefsRepository(
                appContext,
                customerId,
                workContext
            )
        }
    }
}
