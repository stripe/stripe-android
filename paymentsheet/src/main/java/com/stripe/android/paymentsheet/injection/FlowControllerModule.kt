package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.core.injection.IOContext
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.flowcontroller.DefaultPaymentSheetLoader
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.paymentsheet.flowcontroller.PaymentSheetLoader
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [
        PaymentOptionsViewModelSubcomponent::class,
        FormViewModelSubcomponent::class
    ]
)
internal abstract class FlowControllerModule {
    @Binds
    abstract fun bindsPaymentSheetLoader(impl: DefaultPaymentSheetLoader): PaymentSheetLoader

    companion object {

        @Provides
        @Singleton
        fun providePrefsRepositoryFactory(
            appContext: Context,
            @IOContext workContext: CoroutineContext
        ): (PaymentSheet.CustomerConfiguration?) -> PrefsRepository = { customerConfig ->
            DefaultPrefsRepository(
                appContext,
                customerConfig?.id,
                workContext
            )
        }

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
}
