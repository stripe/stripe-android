package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.injection.IOContext
import com.stripe.android.link.LinkIntentConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.DefaultLinkIntentConfirmationHandler
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.PaymentOptionsViewModelSubcomponent
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [
        PaymentOptionsViewModelSubcomponent::class,
    ]
)
internal object FlowControllerModule {
    @Provides
    @Singleton
    fun providesAppContext(application: Application): Context = application.applicationContext

    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    @Provides
    @Singleton
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens() = setOf("PaymentSheet.FlowController")

    @Provides
    @Singleton
    fun provideViewModelScope(viewModel: FlowControllerViewModel): CoroutineScope {
        return viewModel.viewModelScope
    }

    @Provides
    @Singleton
    fun providesSavedStateHandle(
        viewModel: FlowControllerViewModel,
    ): SavedStateHandle {
        return viewModel.handle
    }

    @Provides
    @Singleton
    fun providesConfirmationHandler(
        confirmationHandlerFactory: ConfirmationHandler.Factory,
        viewModel: FlowControllerViewModel,
        @IOContext workContext: CoroutineContext,
    ): ConfirmationHandler {
        return confirmationHandlerFactory.create(
            scope = viewModel.viewModelScope.plus(workContext)
        )
    }

    @Provides
    @Singleton
    fun providesLinkIntentConfirmationHandler(
        confirmationHandler: ConfirmationHandler,
    ): LinkIntentConfirmationHandler {
        return DefaultLinkIntentConfirmationHandler(
            confirmationHandler = confirmationHandler
        )
    }

    @Provides
    @Singleton
    fun provideStripeImageLoader(context: Context): StripeImageLoader {
        return StripeImageLoader(context)
    }

    @Provides
    @Singleton
    @Named(ALLOWS_MANUAL_CONFIRMATION)
    fun provideAllowsManualConfirmation() = true
}
