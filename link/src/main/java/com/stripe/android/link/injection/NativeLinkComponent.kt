package com.stripe.android.link.injection

import android.content.Context
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkIntentConfirmationHandler
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
internal annotation class NativeLinkScope

@NativeLinkScope
@Component(
    modules = [
        NativeLinkModule::class,
    ]
)
internal interface NativeLinkComponent {
    val linkAccountManager: LinkAccountManager
    val configuration: LinkConfiguration
    val linkEventsReporter: LinkEventsReporter
    val logger: Logger
    val viewModel: LinkActivityViewModel
    val linkIntentConfirmationHandler: LinkIntentConfirmationHandler

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun configuration(configuration: LinkConfiguration): Builder

        @BindsInstance
        fun linkIntentConfirmationHandler(linkIntentConfirmationHandler: LinkIntentConfirmationHandler): Builder

        @BindsInstance
        fun publishableKeyProvider(@Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String): Builder

        @BindsInstance
        fun stripeAccountIdProvider(@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?): Builder

        @BindsInstance
        fun context(context: Context): Builder

        fun build(): NativeLinkComponent
    }
}
