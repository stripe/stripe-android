package com.stripe.android.crypto.onramp.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.crypto.onramp.OnrampCoordinator
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        OnrampModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
    ]
)
internal interface OnrampComponent {
    val onrampCoordinator: OnrampCoordinator
    val onrampCallbackIdentifier: String

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
        ): OnrampComponent
    }
}
