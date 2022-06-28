package com.stripe.android.identity.injection

import android.content.Context
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.identity.viewmodel.IdentityViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        IdentityCommonModule::class,
        CoroutineContextModule::class
    ]
)
internal interface IdentityViewModelFactoryComponent {
    fun inject(factory: IdentityViewModel.IdentityViewModelFactory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        fun build(): IdentityViewModelFactoryComponent
    }
}
