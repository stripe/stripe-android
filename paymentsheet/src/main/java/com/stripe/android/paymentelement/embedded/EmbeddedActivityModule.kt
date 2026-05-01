package com.stripe.android.paymentelement.embedded

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.common.spms.DefaultLinkFormElementFactory
import com.stripe.android.common.spms.DefaultLinkInlineSignupAvailability
import com.stripe.android.common.spms.DefaultSavedPaymentMethodLinkFormHelper
import com.stripe.android.common.spms.LinkFormElementFactory
import com.stripe.android.common.spms.LinkInlineSignupAvailability
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.common.taptoadd.DefaultTapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.form.DefaultFormActivityRegistrar
import com.stripe.android.paymentelement.embedded.form.DefaultFormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentelement.embedded.form.FormActivityRegistrar
import com.stripe.android.paymentelement.embedded.form.FormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.OnClickDelegateOverrideImpl
import com.stripe.android.paymentelement.embedded.form.OnClickOverrideDelegate
import com.stripe.android.paymentelement.embedded.manage.DefaultEmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.DefaultEmbeddedUpdateScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedUpdateScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.InitialManageScreenFactory
import com.stripe.android.paymentelement.embedded.manage.ManageSavedPaymentMethodMutatorFactory
import com.stripe.android.paymentelement.embedded.sheet.DefaultSheetActivityConfirmationHelper
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityConfirmationHelper
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.repositories.PrefetchedPaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.verticalmode.DefaultSavedPaymentMethodConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

@Module
internal interface EmbeddedActivityModule {
    @Binds
    fun bindsEmbeddedManageScreenInteractorFactory(
        factory: DefaultEmbeddedManageScreenInteractorFactory
    ): EmbeddedManageScreenInteractorFactory

    @Binds
    fun bindsEmbeddedUpdateScreenInteractorFactory(
        factory: DefaultEmbeddedUpdateScreenInteractorFactory
    ): EmbeddedUpdateScreenInteractorFactory

    @Binds
    fun bindsCardAccountRangeRepositoryFactory(
        defaultCardAccountRangeRepositoryFactory: DefaultCardAccountRangeRepositoryFactory
    ): CardAccountRangeRepository.Factory

    @Binds
    fun bindsUserFacingLogger(impl: RealUserFacingLogger): UserFacingLogger

    @Binds
    fun bindsFormActivityStateHelper(helper: DefaultFormActivityStateHelper): FormActivityStateHelper

    @Binds
    fun bindsPrefsRepositoryFactory(factory: DefaultPrefsRepository.Factory): PrefsRepository.Factory

    @Binds
    fun bindsTapToAddHelperFactory(factory: DefaultTapToAddHelper.Factory): TapToAddHelper.Factory

    @Binds
    fun bindsSavedPaymentMethodLinkFormHelper(
        helper: DefaultSavedPaymentMethodLinkFormHelper
    ): SavedPaymentMethodLinkFormHelper

    @Binds
    fun bindsLinkInlineSignupAvailability(
        impl: DefaultLinkInlineSignupAvailability,
    ): LinkInlineSignupAvailability

    @Binds
    fun providesFormActivityConfirmationHandlerRegistrar(
        implementation: DefaultFormActivityRegistrar
    ): FormActivityRegistrar

    @Binds
    fun bindsConfirmationHelper(
        confirmationHelper: DefaultSheetActivityConfirmationHelper
    ): SheetActivityConfirmationHelper

    @Suppress("TooManyFunctions")
    companion object {
        @Provides
        fun providesContext(application: Application): Context {
            return application
        }

        @Provides
        @Singleton
        @ViewModelScope
        fun provideViewModelScope(): CoroutineScope {
            return CoroutineScope(Dispatchers.Main)
        }

        @Provides
        @Singleton
        fun provideEmbeddedNavigator(
            initialManageScreenFactory: InitialManageScreenFactory,
            @ViewModelScope viewModelScope: CoroutineScope,
            eventReporter: EventReporter,
        ): EmbeddedNavigator {
            return EmbeddedNavigator(
                coroutineScope = viewModelScope,
                eventReporter = eventReporter,
                initialScreen = initialManageScreenFactory.createInitialScreen(),
            )
        }

        @Provides
        @Singleton
        fun provideSavedPaymentMethodMutator(
            factory: ManageSavedPaymentMethodMutatorFactory
        ): SavedPaymentMethodMutator {
            return factory.createSavedPaymentMethodMutator()
        }

        @Provides
        @Singleton
        fun providesLinkAccountHolder(savedStateHandle: SavedStateHandle): LinkAccountHolder {
            return LinkAccountHolder(savedStateHandle)
        }

        @Provides
        @Singleton
        fun provideFormInteractor(
            interactorFactory: EmbeddedFormInteractorFactory
        ): DefaultVerticalModeFormInteractor = interactorFactory.create()

        @Provides
        @Singleton
        fun provideConfirmationHandler(
            confirmationHandlerFactory: ConfirmationHandler.Factory,
            @ViewModelScope coroutineScope: CoroutineScope,
        ): ConfirmationHandler {
            return confirmationHandlerFactory.create(coroutineScope)
        }

        @Provides
        @Singleton
        fun providesTapToAddHelper(
            @ViewModelScope coroutineScope: CoroutineScope,
            configuration: EmbeddedPaymentElement.Configuration,
            tapToAddHelperFactory: TapToAddHelper.Factory,
            embeddedSelectionHolder: EmbeddedSelectionHolder,
            customerStateHolder: CustomerStateHolder,
            paymentMethodMetadata: PaymentMethodMetadata,
        ): TapToAddHelper {
            return tapToAddHelperFactory.create(
                coroutineScope = coroutineScope,
                tapToAddMode = when (configuration.formSheetAction) {
                    EmbeddedPaymentElement.FormSheetAction.Continue -> TapToAddMode.Continue
                    EmbeddedPaymentElement.FormSheetAction.Confirm -> TapToAddMode.Complete
                },
                updateSelection = embeddedSelectionHolder::set,
                customerStateHolder = customerStateHolder,
                linkSignupMode = stateFlowOf(paymentMethodMetadata.linkState?.signupMode),
            )
        }

        @Provides
        @Singleton
        fun provideOnClickOverrideDelegate(): OnClickOverrideDelegate = OnClickDelegateOverrideImpl()

        @Provides
        @Singleton
        fun provideStripeImageLoader(context: Context): StripeImageLoader {
            return DefaultStripeImageLoader(context)
        }

        @Provides
        fun providePaymentMethodMetadataFlow(
            paymentMethodMetadata: PaymentMethodMetadata
        ): StateFlow<PaymentMethodMetadata?> {
            return stateFlowOf(paymentMethodMetadata)
        }

        @Provides
        fun providesTapToAddLinkFormElementFactory(): LinkFormElementFactory {
            return DefaultLinkFormElementFactory
        }

        @Provides
        fun provideSavedPaymentMethodConfirmInteractorFactory(
            @ViewModelScope coroutineScope: CoroutineScope,
            paymentMethodMetadata: PaymentMethodMetadata,
            formActivityStateHelper: FormActivityStateHelper,
            savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
        ): SavedPaymentMethodConfirmInteractor.Factory {
            return DefaultSavedPaymentMethodConfirmInteractor.Factory(
                paymentMethodMetadata = paymentMethodMetadata,
                savedPaymentMethodLinkFormHelper = savedPaymentMethodLinkFormHelper,
                processing = formActivityStateHelper.state.mapAsStateFlow {
                    it.isProcessing
                },
                coroutineScope = coroutineScope,
            )
        }

        @Provides
        fun providesPaymentMethodMessagePromotionHelper(
            promotion: PaymentMethodMessagePromotion?
        ): PaymentMethodMessagePromotionsHelper = PrefetchedPaymentMethodMessagePromotionsHelper(
            listOfNotNull(promotion)
        )
    }
}
