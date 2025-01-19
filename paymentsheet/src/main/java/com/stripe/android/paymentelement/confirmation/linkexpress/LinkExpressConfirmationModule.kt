package com.stripe.android.paymentelement.confirmation.linkexpress

import com.stripe.android.link.LinkExpressLauncher
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationDefinition
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