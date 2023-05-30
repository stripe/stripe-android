package com.stripe.android.link.injection

import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.ui.verification.VerificationViewModel
import dagger.BindsInstance
import dagger.Subcomponent

/**
 * Component that holds the dependency graph for the Link-owned user experience.
 */
@Subcomponent(
    modules = [
        LinkActivityContractArgsModule::class
    ]
)
internal abstract class LinkComponent {
    abstract fun inject(factory: VerificationViewModel.Factory)

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun starterArgs(starterArgs: LinkActivityContract.Args): Builder

        fun build(): LinkComponent
    }
}
