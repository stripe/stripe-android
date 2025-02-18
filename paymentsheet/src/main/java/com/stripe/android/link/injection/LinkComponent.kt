package com.stripe.android.link.injection

import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.gate.LinkGate
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
internal annotation class LinkScope

/**
 * Component that holds the dependency graph for [LinkPaymentLauncher] and related classes used for
 * inline sign up, before Link is launched.
 */
@LinkScope
@Subcomponent(
    modules = [
        LinkModule::class,
        ApplicationIdModule::class,
    ]
)
internal abstract class LinkComponent {
    internal abstract val linkAccountManager: LinkAccountManager
    internal abstract val configuration: LinkConfiguration
    internal abstract val linkGate: LinkGate
    internal abstract val linkAttestationCheck: LinkAttestationCheck
    internal abstract val inlineSignupViewModelFactory: LinkInlineSignupAssistedViewModelFactory

    @Subcomponent.Builder
    internal interface Builder {
        @BindsInstance
        fun configuration(configuration: LinkConfiguration): Builder

        fun build(): LinkComponent
    }
}
