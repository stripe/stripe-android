@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CheckoutControllerModule::class,
    ],
)
internal interface CheckoutControllerComponent {
    val checkoutController: CheckoutController

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance resultCallback: CheckoutController.ResultCallback,
        ): CheckoutControllerComponent
    }
}

@Module
internal object CheckoutControllerModule {
    @Provides
    @Singleton
    @ViewModelScope
    fun provideViewModelScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main)
    }
}
