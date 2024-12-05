package com.stripe.android.paymentelement.confirmation.link

import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkStore
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
internal interface LinkConfirmationModule {
    companion object {
        @JvmSuppressWildcards
        @Provides
        @IntoSet
        fun providesLinkConfirmationDefinition(
            linkStore: LinkStore,
            linkPaymentLauncher: LinkPaymentLauncher,
        ): ConfirmationDefinition<*, *, *, *> {
            return LinkConfirmationDefinition(
                linkStore = linkStore,
                linkPaymentLauncher = linkPaymentLauncher,
            )
        }
    }
}
