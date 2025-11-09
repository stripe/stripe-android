package com.stripe.android.taptoadd

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.DefaultPaymentMethodRefresher
import com.stripe.android.paymentsheet.state.PaymentMethodRefresher
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal interface TapToAddModule {
    @Binds
    fun bindsIsStripeTerminalSdkAvailable(
        isStripeTerminalSdkAvailable: DefaultIsStripeTerminalSdkAvailable
    ): IsStripeTerminalSdkAvailable

    @Binds
    fun bindsPaymentMethodRefresher(
        paymentMethodRefresher: DefaultPaymentMethodRefresher
    ): PaymentMethodRefresher

    companion object {
        @OptIn(TapToAddPreview::class)
        @Singleton
        @Provides
        fun providesTapToAddConnectionManager(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            applicationContext: Context,
            @IOContext workContext: CoroutineContext,
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ) = TapToAddConnectionManager.create(
            isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
            applicationContext = applicationContext,
            workContext = workContext,
            createTerminalSessionCallbackProvider = {
                PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                    ?.createTerminalSessionCallback
            }
        )

        @OptIn(TapToAddPreview::class)
        @Singleton
        @Provides
        fun providesTapToAddCollectionHandler(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            tapToAddConnectionManager: TapToAddConnectionManager,
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ) = TapToAddCollectionHandler.create(
            isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
            connectionManager = tapToAddConnectionManager,
            createCardPresentSetupIntentCallbackProvider = {
                PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                    ?.createCardPresentSetupIntentCallback
            }
        )
    }
}
