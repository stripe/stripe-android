package com.stripe.android.paymentsheet.flowcontroller

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
    fun provideProductUsageTokens(viewModel: FlowControllerViewModel): Set<String> {
        val deferred = viewModel.state?.stripeIntent?.clientSecret == null
        return setOfNotNull(
            "PaymentSheet.FlowController",
            "deferred-intent".takeIf { deferred }
        )
    }

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
}
