package com.stripe.android.paymentelement.confirmation.linkexpress

import com.stripe.android.link.express.LinkExpressLauncher
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module(
    subcomponents = [
        LinkAnalyticsComponent::class,
    ]
)
internal object LinkExpressConfirmationModule {
    @JvmSuppressWildcards
    @Provides
    @IntoSet
    fun providesLinkConfirmationDefinition(
        linkExpressLauncher: LinkExpressLauncher,
    ): ConfirmationDefinition<*, *, *, *> {
        return LinkExpressConfirmationDefinition(
            linkExpressLauncher,
        )
    }
}
