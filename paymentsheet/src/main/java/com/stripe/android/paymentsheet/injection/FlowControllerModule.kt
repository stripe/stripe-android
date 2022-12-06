package com.stripe.android.paymentsheet.injection

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(
    subcomponents = [
        PaymentOptionsViewModelSubcomponent::class,
        FormViewModelSubcomponent::class
    ]
)
internal object FlowControllerModule {

    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    @Provides
    @Singleton
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens() = setOf("PaymentSheet.FlowController")

    @Provides
    @Singleton
    fun provideViewModel(viewModelStoreOwner: ViewModelStoreOwner): FlowControllerViewModel =
        ViewModelProvider(viewModelStoreOwner)[FlowControllerViewModel::class.java]
}
