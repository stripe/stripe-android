package com.stripe.android.link.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkController
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
internal interface LinkControllerComponent {
    val linkController: LinkController

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance @PaymentElementCallbackIdentifier
            paymentElementCallbackIdentifier: String,
        ): LinkControllerComponent
    }
}
