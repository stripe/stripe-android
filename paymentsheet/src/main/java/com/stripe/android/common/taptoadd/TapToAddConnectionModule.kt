package com.stripe.android.common.taptoadd

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@Module
internal interface TapToAddConnectionModule {
    @Binds
    fun bindsStripeTerminalSdkAvailable(
        isStripeTerminalSdkAvailable: DefaultIsStripeTerminalSdkAvailable
    ): IsStripeTerminalSdkAvailable

    companion object {
        @Provides
        fun providesTerminalWrapper(): TerminalWrapper {
            return TerminalWrapper.create()
        }

        @Provides
        fun providesTapToAddConnectionManager(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            errorReporter: ErrorReporter,
            logger: Logger,
            applicationContext: Context,
            paymentConfiguration: Provider<PaymentConfiguration>,
            @IOContext workContext: CoroutineContext
        ): TapToAddConnectionManager {
            return TapToAddConnectionManager.create(
                applicationContext = applicationContext,
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                terminalWrapper = terminalWrapper,
                errorReporter = errorReporter,
                paymentConfiguration = paymentConfiguration,
                isSimulated = BuildConfig.DEBUG,
                logger = logger,
                workContext = workContext,
            )
        }
    }
}
