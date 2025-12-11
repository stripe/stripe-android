package com.stripe.android.common.taptoadd

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@Module
internal interface TapToAddModule {
    @Binds
    fun bindsStripeTerminalSdkAvailable(
        isStripeTerminalSdkAvailable: DefaultIsStripeTerminalSdkAvailable
    ): IsStripeTerminalSdkAvailable

    @Binds
    fun bindsTerminalWrapper(
        terminalWrapper: DefaultTerminalWrapper
    ): TerminalWrapper

    companion object {
        @Provides
        fun providesTapToAddConnectionManager(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            errorReporter: ErrorReporter,
            applicationContext: Context,
            @IOContext workContext: CoroutineContext
        ): TapToAddConnectionManager {
            return TapToAddConnectionManager.create(
                applicationContext = applicationContext,
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                terminalWrapper = terminalWrapper,
                errorReporter = errorReporter,
                isSimulated = BuildConfig.DEBUG,
                workContext = workContext,
            )
        }

        @OptIn(TapToAddPreview::class)
        @Provides
        fun providesTapToAddCollectionHandler(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            connectionManager: TapToAddConnectionManager,
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): TapToAddCollectionHandler {
            return TapToAddCollectionHandler.create(
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                connectionManager = connectionManager,
            )
        }
    }
}
