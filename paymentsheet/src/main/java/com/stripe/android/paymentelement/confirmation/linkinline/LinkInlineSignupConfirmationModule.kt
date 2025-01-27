package com.stripe.android.paymentelement.confirmation.linkinline

import com.stripe.android.link.LinkConfigurationCoordinator
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
internal object LinkInlineSignupConfirmationModule {
    @JvmSuppressWildcards
    @Provides
    @IntoSet
    fun providesLinkConfirmationDefinition(
        linkStore: LinkStore,
        linkConfigurationCoordinator: LinkConfigurationCoordinator,
        linkAnalyticsComponentBuilder: LinkAnalyticsComponent.Builder,
    ): ConfirmationDefinition<*, *, *, *> {
        return LinkInlineSignupConfirmationDefinition(
            linkStore = linkStore,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            linkAnalyticsHelper = linkAnalyticsComponentBuilder.build().linkAnalyticsHelper,
        )
    }
}
