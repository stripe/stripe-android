package com.stripe.android.link.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkControllerViewModel
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        LinkControllerModule::class,
    ]
)
internal interface LinkControllerViewModelComponent {
    val viewModel: LinkControllerViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance @PaymentElementCallbackIdentifier
            paymentElementCallbackIdentifier: String,
        ): LinkControllerViewModelComponent
    }
}
