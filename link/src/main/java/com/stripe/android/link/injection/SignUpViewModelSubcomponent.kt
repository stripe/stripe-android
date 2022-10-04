package com.stripe.android.link.injection

import com.stripe.android.link.ui.signup.SignUpViewModel
import dagger.Subcomponent

/**
 * Subcomponent used by SignUpViewModel.
 */
@Subcomponent
internal interface SignUpViewModelSubcomponent {
    val signUpViewModel: SignUpViewModel

    @Subcomponent.Builder
    interface Builder {
        fun build(): SignUpViewModelSubcomponent
    }
}
