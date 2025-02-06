package com.stripe.android.link.injection

import com.stripe.android.link.express.LinkExpressViewModel
import com.stripe.android.link.model.LinkAccount
import dagger.Module
import dagger.Provides

@Module
internal object LinkExpressViewModelModule {
    @Provides
    @NativeLinkScope
    fun provideLinkExpressViewModel(
        component: NativeLinkComponent,
        linkAccount: LinkAccount?
    ): LinkExpressViewModel? {
        linkAccount ?: return null
        return LinkExpressViewModel(
            linkAccount = linkAccount,
            activityRetainedComponent = component
        )
    }
}
