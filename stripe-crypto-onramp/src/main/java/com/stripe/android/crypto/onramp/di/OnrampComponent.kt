package com.stripe.android.crypto.onramp.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.crypto.onramp.OnrampCoordinator
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        OnrampModule::class,
        StripeRepositoryModule::class,
        ResourceRepositoryModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
    ]
)
internal interface OnrampComponent {
    val onrampCoordinator: OnrampCoordinator

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        fun build(): OnrampComponent
    }
}
