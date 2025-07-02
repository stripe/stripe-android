package com.stripe.android.link.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkPaymentMethodLauncherViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        LinkPaymentMethodLauncherModule::class,
    ]
)
internal interface LinkPaymentMethodLauncherViewModelComponent {
    val viewModel: LinkPaymentMethodLauncherViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
        ): LinkPaymentMethodLauncherViewModelComponent
    }
}
