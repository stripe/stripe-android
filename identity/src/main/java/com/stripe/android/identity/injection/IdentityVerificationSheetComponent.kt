package com.stripe.android.identity.injection

import android.content.Context
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.identity.IdentityActivity
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
internal interface IdentityVerificationSheetComponent {
    fun inject(identityActivity: IdentityActivity)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        fun build(): IdentityVerificationSheetComponent
    }
}
