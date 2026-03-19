package com.stripe.android.paymentelement.embedded.content

import android.app.Application
import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.common.spms.DefaultLinkFormElementFactory
import com.stripe.android.common.spms.DefaultSavedPaymentMethodLinkFormHelper
import com.stripe.android.common.spms.LinkFormElementFactory
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.common.taptoadd.DefaultTapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.paymentelement.embedded.EmbeddedCommonModule
import com.stripe.android.paymentelement.embedded.EmbeddedLinkExtrasModule
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.DefaultFormActivityConfirmationHelper
import com.stripe.android.paymentelement.embedded.form.DefaultFormActivityRegistrar
import com.stripe.android.paymentelement.embedded.form.DefaultFormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentelement.embedded.form.FormActivityConfirmationHelper
import com.stripe.android.paymentelement.embedded.form.FormActivityRegistrar
import com.stripe.android.paymentelement.embedded.form.FormActivityScope
import com.stripe.android.paymentelement.embedded.form.FormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.OnClickDelegateOverrideImpl
import com.stripe.android.paymentelement.embedded.form.OnClickOverrideDelegate
import com.stripe.android.paymentelement.embedded.manage.DefaultEmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.DefaultEmbeddedUpdateScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedUpdateScreenInteractorFactory
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.verticalmode.DefaultSavedPaymentMethodConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Named
import javax.inject.Singleton

@Component(
    modules = [
        ApplicationIdModule::class,
        EmbeddedCommonModule::class,
        EmbeddedSheetModule::class,
        ExtendedPaymentElementConfirmationModule::class,
        GooglePayLauncherModule::class,
        EmbeddedLinkExtrasModule::class,
    ]
)
@Singleton
internal interface EmbeddedSheetComponent {
    val viewModel: EmbeddedSheetViewModel
    val selectionHolder: EmbeddedSelectionHolder
    val customerStateHolder: CustomerStateHolder

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance paymentMethodMetadata: PaymentMethodMetadata,
            @BindsInstance
            @PaymentElementCallbackIdentifier
            paymentElementCallbackIdentifier: String,
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance embeddedSheetArgs: EmbeddedSheetArgs,
        ): EmbeddedSheetComponent
    }
}

@Module(
    subcomponents = [
        EmbeddedSheetSubcomponent::class,
    ]
)
internal interface EmbeddedSheetModule {
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
    fun bindsEmbeddedManageScreenInteractorFactory(
        factory: DefaultEmbeddedManageScreenInteractorFactory
    ): EmbeddedManageScreenInteractorFactory

    @Binds
    fun bindsEmbeddedUpdateScreenInteractorFactory(
        factory: DefaultEmbeddedUpdateScreenInteractorFactory
    ): EmbeddedUpdateScreenInteractorFactory

    @Suppress("TooManyFunctions")
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
        fun providePaymentMethodMetadataFlow(
            paymentMethodMetadata: PaymentMethodMetadata
        ): StateFlow<PaymentMethodMetadata?> {
            return stateFlowOf(paymentMethodMetadata)
        }

        @Provides
        fun providesTapToAddLinkFormElementFactory(): LinkFormElementFactory {
            return DefaultLinkFormElementFactory
        }

        // Form-specific providers. These will throw if accessed in manage mode,
        // which is safe because they're only used through lazy/provider access in form mode.

        @Provides
        @Singleton
        fun provideSelectedPaymentMethodCode(
            embeddedSheetArgs: EmbeddedSheetArgs,
        ): PaymentMethodCode {
            return (embeddedSheetArgs as EmbeddedSheetArgs.Form).formArgs.selectedPaymentMethodCode
        }

        @Provides
        @Singleton
        fun provideHasSavedPaymentMethods(
            embeddedSheetArgs: EmbeddedSheetArgs,
        ): Boolean {
            return (embeddedSheetArgs as EmbeddedSheetArgs.Form).formArgs.hasSavedPaymentMethods
        }

        @Provides
        @Singleton
        fun provideConfiguration(
            embeddedSheetArgs: EmbeddedSheetArgs,
        ): EmbeddedPaymentElement.Configuration {
            return (embeddedSheetArgs as EmbeddedSheetArgs.Form).formArgs.configuration
        }

        @Provides
        @Singleton
        @Named(STATUS_BAR_COLOR)
        fun provideStatusBarColor(
            embeddedSheetArgs: EmbeddedSheetArgs,
        ): Int? {
            return (embeddedSheetArgs as EmbeddedSheetArgs.Form).formArgs.statusBarColor
        }

        @Provides
        @Singleton
        fun provideFormInteractor(
            interactorFactory: dagger.Lazy<EmbeddedFormInteractorFactory>,
            embeddedSheetArgs: EmbeddedSheetArgs,
        ): DefaultVerticalModeFormInteractor {
            check(embeddedSheetArgs is EmbeddedSheetArgs.Form) { "FormInteractor only available in form mode" }
            return interactorFactory.get().create()
        }

        @Provides
        @Singleton
        fun providesTapToAddHelper(
            @ViewModelScope coroutineScope: CoroutineScope,
            embeddedSheetArgs: EmbeddedSheetArgs,
            tapToAddHelperFactory: dagger.Lazy<TapToAddHelper.Factory>,
            embeddedSelectionHolder: EmbeddedSelectionHolder,
            customerStateHolder: CustomerStateHolder,
            paymentMethodMetadata: PaymentMethodMetadata,
        ): TapToAddHelper {
            check(embeddedSheetArgs is EmbeddedSheetArgs.Form) { "TapToAddHelper only available in form mode" }
            val configuration = embeddedSheetArgs.formArgs.configuration
            return tapToAddHelperFactory.get().create(
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
        fun providesFormActivityConfirmationHandlerRegistrar(
            confirmationHandler: ConfirmationHandler,
            tapToAddHelper: dagger.Lazy<TapToAddHelper>,
            embeddedSheetArgs: EmbeddedSheetArgs,
        ): FormActivityRegistrar {
            check(embeddedSheetArgs is EmbeddedSheetArgs.Form) { "FormActivityRegistrar only available in form mode" }
            return DefaultFormActivityRegistrar(confirmationHandler, tapToAddHelper.get())
        }

        @Provides
        fun provideSavedPaymentMethodConfirmInteractorFactory(
            @ViewModelScope coroutineScope: CoroutineScope,
            paymentMethodMetadata: PaymentMethodMetadata,
            savedPaymentMethodLinkFormHelper: dagger.Lazy<SavedPaymentMethodLinkFormHelper>,
            embeddedSheetArgs: EmbeddedSheetArgs,
        ): SavedPaymentMethodConfirmInteractor.Factory {
            check(embeddedSheetArgs is EmbeddedSheetArgs.Form) {
                "SavedPaymentMethodConfirmInteractor.Factory only available in form mode"
            }
            return DefaultSavedPaymentMethodConfirmInteractor.Factory(
                paymentMethodMetadata = paymentMethodMetadata,
                savedPaymentMethodLinkFormHelper = savedPaymentMethodLinkFormHelper.get(),
                coroutineScope = coroutineScope,
            )
        }
    }
}

@Subcomponent(
    modules = [
        EmbeddedSheetActivityModule::class,
    ]
)
@FormActivityScope
internal interface EmbeddedSheetSubcomponent {
    val confirmationHelper: FormActivityConfirmationHelper

    @Subcomponent.Factory
    interface Factory {
        fun build(
            @BindsInstance activityResultCaller: ActivityResultCaller,
            @BindsInstance lifecycleOwner: LifecycleOwner,
        ): EmbeddedSheetSubcomponent
    }
}

@Module
internal interface EmbeddedSheetActivityModule {
    @Binds
    fun bindsFormConfirmationHelper(
        confirmationHandler: DefaultFormActivityConfirmationHelper
    ): FormActivityConfirmationHelper
}
