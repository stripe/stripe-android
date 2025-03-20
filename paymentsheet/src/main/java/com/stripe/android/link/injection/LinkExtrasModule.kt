package com.stripe.android.link.injection

import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.RealLinkConfigurationCoordinator
import com.stripe.android.link.gate.DefaultLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.paymentsheet.state.DefaultLinkAccountStatusProvider
import com.stripe.android.paymentsheet.state.LinkAccountStatusProvider
import dagger.Binds
import dagger.Module

@Module(
    subcomponents = [
        LinkAnalyticsComponent::class,
        LinkComponent::class,
    ]
)
internal interface LinkExtrasModule {
    @Binds
    fun bindLinkGateFactory(linkGateFactory: DefaultLinkGate.Factory): LinkGate.Factory

    @Binds
    fun bindsLinkConfigurationCoordinator(impl: RealLinkConfigurationCoordinator): LinkConfigurationCoordinator

    @Binds
    fun bindsLinkAccountStatusProvider(impl: DefaultLinkAccountStatusProvider): LinkAccountStatusProvider
}
