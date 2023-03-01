package com.stripe.android.paymentsheet.wallet.embeddable

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface SavedPaymentMethodsViewModelSubcomponent {
    @OptIn(ExperimentalPaymentSheetDecouplingApi::class)
    val savedPaymentMethodsViewModel: SavedPaymentMethodsViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder


        fun build(): SavedPaymentMethodsViewModelSubcomponent
    }
}
