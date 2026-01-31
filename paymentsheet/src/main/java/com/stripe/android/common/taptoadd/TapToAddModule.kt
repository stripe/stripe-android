package com.stripe.android.common.taptoadd

import android.content.Context
import android.os.Build
import com.stripe.android.common.taptoadd.nfcdirect.DefaultIsNfcDirectAvailable
import com.stripe.android.common.taptoadd.nfcdirect.IsNfcDirectAvailable
import com.stripe.android.common.taptoadd.nfcdirect.NfcDirectConnectionManager
import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@OptIn(TapToAddPreview::class)
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

    @Binds
    fun bindsCreateCardPresentSetupIntentCallbackRetriever(
        retriever: DefaultCreateCardPresentSetupIntentCallbackRetriever
    ): CreateCardPresentSetupIntentCallbackRetriever

    @Binds
    fun bindsIsNfcDirectAvailable(
        isNfcDirectAvailable: DefaultIsNfcDirectAvailable
    ): IsNfcDirectAvailable

    companion object {
        @Provides
        fun providesCreateCardPresentSetupIntentCallback(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): CreateCardPresentSetupIntentCallback? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.createCardPresentSetupIntentCallback
        }

        @Provides
        fun providesTapToAddConnectionManager(
            isNfcDirectAvailable: IsNfcDirectAvailable,
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            errorReporter: ErrorReporter,
            applicationContext: Context,
            @IOContext workContext: CoroutineContext
        ): TapToAddConnectionManager {
            // Prefer NFC Direct (lightweight) over Terminal SDK when available
            if (isNfcDirectAvailable() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return NfcDirectConnectionManager(
                    context = applicationContext,
                    workContext = workContext,
                )
            }

            // Fall back to Terminal SDK
            return TapToAddConnectionManager.create(
                applicationContext = applicationContext,
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                terminalWrapper = terminalWrapper,
                errorReporter = errorReporter,
                isSimulated = BuildConfig.DEBUG,
                workContext = workContext,
            )
        }

        @Provides
        fun providesTapToAddCollectionHandler(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            connectionManager: TapToAddConnectionManager,
            terminalWrapper: TerminalWrapper,
            errorReporter: ErrorReporter,
            createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever
        ): TapToAddCollectionHandler {
            return TapToAddCollectionHandler.create(
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                connectionManager = connectionManager,
                terminalWrapper = terminalWrapper,
                errorReporter = errorReporter,
                createCardPresentSetupIntentCallbackRetriever = createCardPresentSetupIntentCallbackRetriever,
            )
        }
    }
}
