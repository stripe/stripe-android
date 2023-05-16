package com.stripe.android.paymentsheet.wallet.sheet

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.PaymentOptionsViewModelSubcomponent
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import javax.inject.Singleton

@Module(
    subcomponents = [
        SavedPaymentMethodsSheetViewModelSubcomponent::class,
        FormViewModelSubcomponent::class
    ]
)
internal object SavedPaymentMethodsSheetModule {

    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    @Provides
    @Singleton
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens() = setOf("SavedPaymentMethods")

    @Provides
    @Singleton
    fun provideViewModelScope(viewModel: SavedPaymentMethodsViewModel): CoroutineScope {
        return viewModel.viewModelScope
    }

    @Provides
    @Singleton
    fun provideStripeImageLoader(context: Context): StripeImageLoader {
        return StripeImageLoader(context)
    }
}
