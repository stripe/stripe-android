package com.stripe.android.link.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkControllerViewModel
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
        ): LinkControllerViewModelComponent
    }
}
