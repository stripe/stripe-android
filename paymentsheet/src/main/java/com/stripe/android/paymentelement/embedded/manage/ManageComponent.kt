package com.stripe.android.paymentelement.embedded.manage

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedCommonModule
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Component(
    modules = [
        ManageModule::class,
        EmbeddedCommonModule::class,
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
        ): ManageNavigator {
            return ManageNavigator(viewModelScope, initialManageScreenFactory.createInitialScreen())
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
        fun provideViewModelScope(@IOContext ioContext: CoroutineContext): CoroutineScope {
            return CoroutineScope(ioContext)
        }
    }
}
