package com.stripe.android.link.injection

import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.ui.signup.SignUpViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface SignUpViewModelSubcomponent {
    val viewModel: SignUpViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun args(args: LinkActivityContract.Args): Builder

        fun build(): SignUpViewModelSubcomponent
    }
}
