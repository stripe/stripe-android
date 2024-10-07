package com.stripe.android.customersheet.data.injection

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.data.CustomerSheetInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import com.stripe.android.customersheet.injection.CustomerSheetDataCommonModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@OptIn(ExperimentalCustomerSessionApi::class)
@Singleton
@Component(
    modules = [
        CustomerSessionDataSourceModule::class,
        CustomerSheetDataSourceCommonModule::class,
        CustomerSheetDataCommonModule::class,
        StripeRepositoryModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
    ]
)
internal interface CustomerSessionDataSourceComponent {
    val customerSheetPaymentMethodDataSource: CustomerSheetPaymentMethodDataSource
    val customerSheetSavedSelectionDataSource: CustomerSheetSavedSelectionDataSource
    val customerSheetIntentDataSource: CustomerSheetIntentDataSource
    val customerSheetInitializationDataSource: CustomerSheetInitializationDataSource

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun customerSessionProvider(customerSessionProvider: CustomerSheet.CustomerSessionProvider): Builder

        fun build(): CustomerSessionDataSourceComponent
    }
}
