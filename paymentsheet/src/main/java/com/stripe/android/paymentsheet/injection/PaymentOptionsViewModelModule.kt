package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.analytics.EventReporter
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal class PaymentOptionsViewModelModule {

    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    @Provides
    fun provideContext(application: Application): Context = application

    @Provides
    @PaymentElementCallbackIdentifier
    fun provideCallbackIdentifier(args: PaymentOptionContract.Args): String = args.paymentElementCallbackIdentifier

    @Provides
    @Named(PRODUCT_USAGE)
    fun provideProductUsage(args: PaymentOptionContract.Args): Set<String> = args.productUsage

    @Provides
    fun providePaymentMethodMetadata(args: PaymentOptionContract.Args): PaymentMethodMetadata {
        return args.state.paymentMethodMetadata
    }
}
