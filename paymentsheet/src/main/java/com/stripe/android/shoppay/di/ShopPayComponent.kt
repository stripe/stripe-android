package com.stripe.android.shoppay.di

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.shoppay.ShopPayArgs
import com.stripe.android.shoppay.ShopPayViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ShopPayModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
    ]
)
internal interface ShopPayComponent {
    val viewModel: ShopPayViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance context: Context,
            @BindsInstance shopPayArgs: ShopPayArgs,
        ): ShopPayComponent
    }
}
