package com.stripe.android.link.injection

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.paymentelement.confirmation.injection.DefaultConfirmationModule
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
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
        LinkViewModelModule::class,
        DefaultConfirmationModule::class,
    ]
)
internal interface NativeLinkComponent {
    val linkAccountManager: LinkAccountManager
    val configuration: LinkConfiguration
    val linkEventsReporter: LinkEventsReporter
    val logger: Logger
    val linkConfirmationHandlerFactory: LinkConfirmationHandler.Factory
    val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory
    val viewModel: LinkActivityViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun configuration(configuration: LinkConfiguration): Builder

        @BindsInstance
        fun publishableKeyProvider(@Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String): Builder

        @BindsInstance
        fun stripeAccountIdProvider(@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?): Builder

        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun linkAccount(linkAccount: LinkAccount?): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        @BindsInstance
        fun statusBarColor(@Named(STATUS_BAR_COLOR) statusBarColor: Int?): Builder

        fun build(): NativeLinkComponent
    }
}
