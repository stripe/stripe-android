package com.stripe.android.paymentelement.embedded.manage

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.EmbeddedCommonModule
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.PaymentSheetLauncherComponent.Builder
import com.stripe.android.ui.core.di.CardScanModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Component(
    modules = [
        ManageModule::class,
        EmbeddedCommonModule::class,
        CardScanModule::class
    ],
)
@Singleton
internal interface ManageComponent {
    val viewModel: ManageViewModel
    val customerStateHolder: CustomerStateHolder
    val selectionHolder: EmbeddedSelectionHolder
    fun inject(activity: ManageActivity)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        @BindsInstance
        fun paymentMethodMetadata(paymentMethodMetadata: PaymentMethodMetadata): Builder

        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun paymentElementCallbackIdentifier(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String
        ): Builder

        fun build(): ManageComponent
    }
}

@Module
internal interface ManageModule {
    @Binds
    fun bindsEmbeddedManageScreenInteractorFactory(
        factory: DefaultEmbeddedManageScreenInteractorFactory
    ): EmbeddedManageScreenInteractorFactory

    @Binds
    fun bindsEmbeddedUpdateScreenInteractorFactory(
        factory: DefaultEmbeddedUpdateScreenInteractorFactory
    ): EmbeddedUpdateScreenInteractorFactory

    companion object {
        @Provides
        @Singleton
        fun provideManageNavigator(
            initialManageScreenFactory: InitialManageScreenFactory,
            @ViewModelScope viewModelScope: CoroutineScope,
            eventReporter: EventReporter,
        ): ManageNavigator {
            return ManageNavigator(
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
        @ViewModelScope
        fun provideViewModelScope(): CoroutineScope {
            return CoroutineScope(Dispatchers.Main)
        }
    }
}
