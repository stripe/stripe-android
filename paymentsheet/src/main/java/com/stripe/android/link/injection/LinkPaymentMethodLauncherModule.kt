package com.stripe.android.link.injection

import android.app.Application
import android.content.Context
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.common.di.MobileSessionIdModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.LinkPaymentMethodLauncher
import com.stripe.android.link.RealLinkPaymentMethodLauncher
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.LinkHoldbackExposureModule
import com.stripe.android.paymentsheet.injection.PaymentSheetCommonModule
import com.stripe.android.ui.core.di.CardScanModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(
    includes = [
        StripeRepositoryModule::class,
        ExtendedPaymentElementConfirmationModule::class,
        PaymentSheetCommonModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
        ApplicationIdModule::class,
        MobileSessionIdModule::class,
        CardScanModule::class,
        LinkHoldbackExposureModule::class
    ]
)
internal object LinkPaymentMethodLauncherModule {
    @Provides
    @Singleton
    fun provideAppContext(application: Application): Context = application.applicationContext

    // TODO
    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    @Provides
    @Singleton
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens() = setOf("LinkPaymentMethodLauncher")

    @Provides
    fun provideLinkPaymentMethodLauncher(impl: RealLinkPaymentMethodLauncher): LinkPaymentMethodLauncher = impl

    @Provides
    @PaymentElementCallbackIdentifier
    fun providePaymentElementCallbackIdentifier(): String = "LinkPaymentMethodLauncher"
}
