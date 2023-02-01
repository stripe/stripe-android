package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.ConfirmationHandler
import com.stripe.android.paymentsheet.DefaultConfirmationHandler
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import javax.inject.Singleton

@Module(
    subcomponents = [
        PaymentOptionsViewModelSubcomponent::class,
        FormViewModelSubcomponent::class,
    ],
    includes = [
        FlowControllerModule.ConfirmationHandlerModule::class,
    ],
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

    @Provides
    @Singleton
    fun provideViewModelScope(viewModel: FlowControllerViewModel): CoroutineScope {
        return viewModel.viewModelScope
    }

    @Provides
    @Singleton
    fun provideStripeImageLoader(context: Context): StripeImageLoader {
        return StripeImageLoader(context)
    }

    @Module
    interface ConfirmationHandlerModule {

        @Binds
        fun bindConfirmationHandler(impl: DefaultConfirmationHandler): ConfirmationHandler
    }
}
