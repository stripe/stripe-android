package com.stripe.android.link.injection

import com.stripe.android.link.LinkActivityContract
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Module that extracts variables needed for injection from [LinkActivityContract.Args].
 */
@Module(
    subcomponents = [
        FormControllerSubcomponent::class
    ]
)
internal interface LinkActivityContractArgsModule {
    companion object {
        @Provides
        @Singleton
        fun provideConfiguration(args: LinkActivityContract.Args) = args.configuration
    }
}
