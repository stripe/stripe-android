package com.stripe.android.link.injection

import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.verification.VerificationViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface LinkViewModelSubcomponent {
    val linkActivityViewModel: LinkActivityViewModel
    val signUpViewModel: SignUpViewModel
    val verificationViewModel: VerificationViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun args(args: LinkActivityContract.Args): Builder

        fun build(): LinkViewModelSubcomponent
    }
}
