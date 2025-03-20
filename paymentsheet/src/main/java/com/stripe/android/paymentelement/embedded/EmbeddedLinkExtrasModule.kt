package com.stripe.android.paymentelement.embedded

import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.RealLinkConfigurationCoordinator
import com.stripe.android.link.gate.DefaultLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.injection.LinkComponent
import dagger.Binds
import dagger.Module

@Module(
    subcomponents = [
        LinkAnalyticsComponent::class,
        LinkComponent::class,
    ]
)
internal interface EmbeddedLinkExtrasModule {
    @Binds
    fun bindLinkGateFactory(linkGateFactory: DefaultLinkGate.Factory): LinkGate.Factory

    @Binds
    fun bindsLinkConfigurationCoordinator(impl: RealLinkConfigurationCoordinator): LinkConfigurationCoordinator
}
