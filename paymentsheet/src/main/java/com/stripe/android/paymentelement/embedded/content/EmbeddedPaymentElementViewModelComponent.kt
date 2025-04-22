package com.stripe.android.paymentelement.embedded.content

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.common.di.MobileSessionIdModule
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.paymentelement.embedded.EmbeddedCommonModule
import com.stripe.android.paymentelement.embedded.EmbeddedLinkExtrasModule
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.injection.LinkHoldbackExposureModule
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.RealElementsSessionRepository
import com.stripe.android.paymentsheet.state.DefaultLinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.DefaultPaymentElementLoader
import com.stripe.android.paymentsheet.state.DefaultRetrieveCustomerEmail
import com.stripe.android.paymentsheet.state.LinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.RetrieveCustomerEmail
import com.stripe.android.ui.core.di.CardScanModule
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@ExperimentalEmbeddedPaymentElementApi
@Singleton
@Component(
    modules = [
        EmbeddedPaymentElementViewModelModule::class,
        GooglePayLauncherModule::class,
        ExtendedPaymentElementConfirmationModule::class,
        EmbeddedCommonModule::class,
        ApplicationIdModule::class,
        MobileSessionIdModule::class,
        CardScanModule::class,
        EmbeddedLinkExtrasModule::class,
        LinkHoldbackExposureModule::class,
    ],
)
internal interface EmbeddedPaymentElementViewModelComponent {
    val viewModel: EmbeddedPaymentElementViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance application: Application,
            @BindsInstance @PaymentElementCallbackIdentifier
            paymentElementCallbackIdentifier: String,
            @BindsInstance
            @Named(STATUS_BAR_COLOR)
            statusBarColor: Int?,
        ): EmbeddedPaymentElementViewModelComponent
    }
}

@ExperimentalEmbeddedPaymentElementApi
@Module(
    subcomponents = [
        EmbeddedPaymentElementSubcomponent::class,
    ],
)
internal interface EmbeddedPaymentElementViewModelModule {
    @Binds
    fun bindsEmbeddedStateHelper(
        stateHelper: DefaultEmbeddedStateHelper
    ): EmbeddedStateHelper

    @Binds
    fun bindsPaymentOptionDisplayDataHolder(
        paymentOptionDisplayDataHolder: DefaultPaymentOptionDisplayDataHolder
    ): PaymentOptionDisplayDataHolder

    @Binds
    fun bindConfigurationCoordinator(
        configurationCoordinator: DefaultEmbeddedConfigurationCoordinator
    ): EmbeddedConfigurationCoordinator

    @Binds
    fun bindsCardAccountRangeRepositoryFactory(
        defaultCardAccountRangeRepositoryFactory: DefaultCardAccountRangeRepositoryFactory
    ): CardAccountRangeRepository.Factory

    @Binds
    fun bindsConfigurationHandler(
        handler: DefaultEmbeddedConfigurationHandler
    ): EmbeddedConfigurationHandler

    @Binds
    fun bindsWalletsHelper(helper: DefaultEmbeddedWalletsHelper): EmbeddedWalletsHelper

    @Binds
    fun bindsElementsSessionRepository(impl: RealElementsSessionRepository): ElementsSessionRepository

    @Binds
    fun bindPaymentElementLoader(loader: DefaultPaymentElementLoader): PaymentElementLoader

    @Binds
    fun bindRetrieveCustomerEmail(
        retrieveCustomerEmail: DefaultRetrieveCustomerEmail
    ): RetrieveCustomerEmail

    @Binds
    fun bindSelectionChooser(chooser: DefaultEmbeddedSelectionChooser): EmbeddedSelectionChooser

    @Binds
    fun bindsUserFacingLogger(impl: RealUserFacingLogger): UserFacingLogger

    @Binds
    fun bindsLinkAccountStatusProvider(
        impl: DefaultLinkAccountStatusProvider,
    ): LinkAccountStatusProvider

    @Binds
    fun bindsEmbeddedContentHelper(helper: DefaultEmbeddedContentHelper): EmbeddedContentHelper

    companion object {
        @Provides
        fun providesContext(application: Application): Context {
            return application
        }

        @Provides
        @Singleton
        fun providesLinkAccountHolder(savedStateHandle: SavedStateHandle): LinkAccountHolder {
            return LinkAccountHolder(savedStateHandle)
        }

        @Provides
        fun providePrefsRepositoryFactory(
            appContext: Context,
            @IOContext workContext: CoroutineContext
        ): (PaymentSheet.CustomerConfiguration?) -> PrefsRepository = { customerConfig ->
            DefaultPrefsRepository(
                appContext,
                customerConfig?.id,
                workContext
            )
        }

        @Provides
        fun provideResources(context: Context): Resources {
            return context.resources
        }

        @Provides
        @Singleton
        fun provideStripeImageLoader(context: Context): StripeImageLoader {
            return StripeImageLoader(context)
        }

        @Provides
        @Singleton
        @ViewModelScope
        fun provideViewModelScope(): CoroutineScope {
            return CoroutineScope(Dispatchers.Main)
        }

        @Provides
        @Singleton
        fun provideConfirmationHandler(
            confirmationHandlerFactory: ConfirmationHandler.Factory,
            @ViewModelScope coroutineScope: CoroutineScope,
        ): ConfirmationHandler {
            return confirmationHandlerFactory.create(coroutineScope)
        }

        @Provides
        fun providesConfirmationStateSupplier(
            confirmationStateHolder: EmbeddedConfirmationStateHolder,
        ): () -> EmbeddedConfirmationStateHolder.State? {
            return { confirmationStateHolder.state }
        }
    }
}
