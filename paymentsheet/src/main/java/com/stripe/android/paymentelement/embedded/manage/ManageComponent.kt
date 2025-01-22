package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Component(
    modules = [
        ManageModule::class,
    ],
)
@Singleton
internal interface ManageComponent {

    val viewModel: ManageViewModel

    val customerStateHolder: CustomerStateHolder

    val selectionHolder: EmbeddedSelectionHolder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        @BindsInstance
        fun paymentMethodMetadata(paymentMethodMetadata: PaymentMethodMetadata): Builder

        fun build(): ManageComponent
    }
}

@Module
internal object ManageModule {
    @Provides
    @Singleton
    fun providesCustomerStateHolder(
        savedStateHandle: SavedStateHandle,
        selectionHolder: EmbeddedSelectionHolder,
    ): CustomerStateHolder {
        return CustomerStateHolder(savedStateHandle, selectionHolder.selection)
    }
}
