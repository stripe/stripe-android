package com.stripe.android.crypto.onramp.di

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.crypto.onramp.OnrampCoordinator
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@OnrampPresenterScope
@Subcomponent(
    modules = [OnrampPresenterModule::class]
)
internal interface OnrampPresenterComponent {
    val presenter: OnrampCoordinator.Presenter

    @Subcomponent.Factory
    interface Factory {
        fun build(
            @BindsInstance activity: ComponentActivity,
            @BindsInstance lifecycleOwner: LifecycleOwner,
            @BindsInstance activityResultRegistryOwner: ActivityResultRegistryOwner,
            @BindsInstance onrampCallbacks: OnrampCallbacks,
        ): OnrampPresenterComponent
    }
}

@Module
internal object OnrampPresenterModule {
    @Provides
    @OnrampPresenterScope
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
