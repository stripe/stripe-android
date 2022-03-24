package com.stripe.android.link.injection

import com.stripe.android.link.ui.signup.SignUpViewModel
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

/**
 * Subcomponent used by SignUpViewModel.
 */
@Subcomponent
internal interface SignUpViewModelSubcomponent {
    val signUpViewModel: SignUpViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun prefilledEmail(@Named(SignUpViewModel.PREFILLED_EMAIL) prefilledEmail: String?): Builder

        fun build(): SignUpViewModelSubcomponent
    }
}
