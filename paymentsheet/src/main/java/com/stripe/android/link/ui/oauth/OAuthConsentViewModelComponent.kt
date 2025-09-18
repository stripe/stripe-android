package com.stripe.android.link.ui.oauth

import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.model.LinkAccount
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface OAuthConsentViewModelComponent {
    val viewModel: OAuthConsentViewModel

    @Subcomponent.Factory
    interface Factory {
        fun build(
            @BindsInstance linkAccount: LinkAccount,
            @BindsInstance dismissWithResult: (LinkActivityResult) -> Unit,
        ): OAuthConsentViewModelComponent
    }
}
