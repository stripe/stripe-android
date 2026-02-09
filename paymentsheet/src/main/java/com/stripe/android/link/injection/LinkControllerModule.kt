package com.stripe.android.link.injection

import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.common.di.MobileSessionIdModule
import com.stripe.android.core.injection.ApplicationContextModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.DefaultLinkConfigurationLoader
import com.stripe.android.link.LinkConfigurationLoader
import com.stripe.android.link.LinkControllerInteractor
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.LinkHoldbackExposureModule
import com.stripe.android.paymentsheet.injection.PaymentSheetCommonModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(
    includes = [
        ApplicationContextModule::class,
        StripeRepositoryModule::class,
        ExtendedPaymentElementConfirmationModule::class,
        PaymentSheetCommonModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
        ApplicationIdModule::class,
        MobileSessionIdModule::class,
        LinkHoldbackExposureModule::class,
        PaymentsIntegrityModule::class,
    ],
    subcomponents = [
        LinkControllerPresenterComponent::class,
        LinkComponent::class,
    ]
)
internal interface LinkControllerModule {
    @Binds
    @Singleton
    fun bindLinkConfigurationLoader(impl: DefaultLinkConfigurationLoader): LinkConfigurationLoader

    companion object {
        @Provides
        @Singleton
        fun providePaymentMethodMetadata(interactor: LinkControllerInteractor): PaymentMethodMetadata? {
            return interactor.paymentMethodMetadata
        }

        // TODO
        @Provides
        @Singleton
        fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

        @Provides
        @Singleton
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens() = setOf("LinkPaymentMethodLauncher")
    }
}
