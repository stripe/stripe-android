package com.stripe.android.customersheet.data.injection

import android.app.Application
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.common.di.MobileSessionIdModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.data.CustomerSheetInitializationDataSource
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import com.stripe.android.customersheet.injection.CustomerSheetDataCommonModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CustomerAdapterDataSourceModule::class,
        CustomerSheetDataSourceCommonModule::class,
        CustomerSheetDataCommonModule::class,
        StripeRepositoryModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ApplicationIdModule::class,
        MobileSessionIdModule::class,
    ]
)
internal interface CustomerAdapterDataSourceComponent {
    val customerSheetPaymentMethodDataSource: CustomerSheetPaymentMethodDataSource
    val customerSheetSavedSelectionDataSource: CustomerSheetSavedSelectionDataSource
    val customerSheetIntentDataSource: CustomerSheetIntentDataSource
    val customerSheetInitializationDataSource: CustomerSheetInitializationDataSource

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun adapter(customerAdapter: CustomerAdapter): Builder

        fun build(): CustomerAdapterDataSourceComponent
    }
}
