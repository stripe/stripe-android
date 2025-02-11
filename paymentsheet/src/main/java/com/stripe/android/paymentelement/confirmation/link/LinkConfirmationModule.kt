package com.stripe.android.paymentelement.confirmation.link

import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module(
    subcomponents = [
        LinkAnalyticsComponent::class,
    ]
)
internal interface LinkConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsLinkConfirmationDefinition(
        definition: LinkConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>
}
