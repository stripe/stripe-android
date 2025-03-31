package com.stripe.android.paymentelement.embedded.form

import android.app.Application
import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.paymentelement.embedded.EmbeddedCommonModule
import com.stripe.android.paymentelement.embedded.EmbeddedLinkExtrasModule
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.ui.core.di.CardScanModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Component(
    modules = [
        EmbeddedCommonModule::class,
        FormActivityViewModelModule::class,
        ExtendedPaymentElementConfirmationModule::class,
        GooglePayLauncherModule::class,
        CardScanModule::class,
        EmbeddedLinkExtrasModule::class
    ]
)
@Singleton
@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal interface FormActivityViewModelComponent {
    val viewModel: FormActivityViewModel
    val subcomponentBuilder: FormActivitySubcomponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun paymentMethodMetadata(paymentMethodMetadata: PaymentMethodMetadata): Builder

        @BindsInstance
        fun selectedPaymentMethodCode(selectedPaymentMethodCode: PaymentMethodCode): Builder

        @BindsInstance
        fun hasSavedPaymentMethods(hasSavedPaymentMethods: Boolean): Builder

        @BindsInstance
        fun statusBarColor(@Named(STATUS_BAR_COLOR) statusBarColor: Int?): Builder

        @BindsInstance
        fun configuration(configuration: EmbeddedPaymentElement.Configuration): Builder

        @BindsInstance
        fun initializationMode(initializationMode: PaymentElementLoader.InitializationMode): Builder

        @BindsInstance
        fun paymentElementCallbackIdentifier(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): Builder

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        fun build(): FormActivityViewModelComponent
    }
}

@Module(
    subcomponents = [
        FormActivitySubcomponent::class
    ]
)
internal interface FormActivityViewModelModule {
    @Binds
    fun bindsCardAccountRangeRepositoryFactory(
        defaultCardAccountRangeRepositoryFactory: DefaultCardAccountRangeRepositoryFactory
    ): CardAccountRangeRepository.Factory

    @Binds
    fun bindsUserFacingLogger(impl: RealUserFacingLogger): UserFacingLogger

    @Binds
    fun bindsFormActivityStateHelper(helper: DefaultFormActivityStateHelper): FormActivityStateHelper

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
        fun provideFormInteractor(
            interactorFactory: EmbeddedFormInteractorFactory
        ): DefaultVerticalModeFormInteractor = interactorFactory.create()

        @Provides
        @Singleton
        fun provideConfirmationHandler(
            confirmationHandlerFactory: ConfirmationHandler.Factory,
            @ViewModelScope coroutineScope: CoroutineScope,
            @IOContext ioContext: CoroutineContext,
        ): ConfirmationHandler {
            return confirmationHandlerFactory.create(coroutineScope + ioContext)
        }

        @Provides
        @Singleton
        fun provideOnClickOverrideDelegate(): OnClickOverrideDelegate = OnClickDelegateOverrideImpl()
    }
}

@Subcomponent(
    modules = [
        FormActivityModule::class,
    ]
)
@FormActivityScope
internal interface FormActivitySubcomponent {
    fun inject(activity: FormActivity)

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun activityResultCaller(activityResultCaller: ActivityResultCaller): Builder

        @BindsInstance
        fun lifecycleOwner(lifecycleOwner: LifecycleOwner): Builder

        fun build(): FormActivitySubcomponent
    }
}

@Module
internal interface FormActivityModule {
    @Binds
    fun bindsFormConfirmationHelper(
        confirmationHandler: DefaultFormActivityConfirmationHelper
    ): FormActivityConfirmationHelper
}
