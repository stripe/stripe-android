package com.stripe.android.link.injection

import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.VerificationViewModel
import dagger.BindsInstance
import dagger.Subcomponent

/**
 * Subcomponent used by ViewModels that require a [LinkAccount].
 */
@Subcomponent
internal interface SignedInViewModelSubcomponent {
    val verificationViewModel: VerificationViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun linkAccount(linkAccount: LinkAccount): Builder

        fun build(): SignedInViewModelSubcomponent
    }
}
