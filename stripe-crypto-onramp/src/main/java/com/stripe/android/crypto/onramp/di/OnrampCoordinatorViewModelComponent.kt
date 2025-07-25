package com.stripe.android.crypto.onramp.di

import com.stripe.android.crypto.onramp.viewmodels.OnrampCoordinatorViewModel
import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.link.LinkController
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        OnrampModule::class
    ]
)
internal interface OnrampCoordinatorViewModelComponent {
    val viewModel: OnrampCoordinatorViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance linkController: LinkController,
            @BindsInstance onrampCallbacks: OnrampCallbacks
        ): OnrampCoordinatorViewModelComponent
    }
}
