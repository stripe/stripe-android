package com.stripe.android.link.injection

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.WebLinkActivityContract
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.injection.DefaultConfirmationModule
import com.stripe.android.paymentelement.confirmation.link.LinkPassthroughConfirmationModule
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.addresselement.AutocompleteLauncher
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.ui.core.di.CardScanModule
import com.stripe.android.uicore.navigation.NavigationManager
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
        ApplicationIdModule::class,
        DefaultConfirmationModule::class,
        LinkPassthroughConfirmationModule::class,
        CardScanModule::class
    ]
)
internal interface NativeLinkComponent {
    val linkAccountManager: LinkAccountManager
    val configuration: LinkConfiguration
    val linkEventsReporter: LinkEventsReporter
    val errorReporter: ErrorReporter
    val logger: Logger
    val linkConfirmationHandlerFactory: LinkConfirmationHandler.Factory
    val webLinkActivityContract: WebLinkActivityContract
    val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory
    val linkAuth: LinkAuth
    val savedStateHandle: SavedStateHandle
    val viewModel: LinkActivityViewModel
    val eventReporter: EventReporter
    val navigationManager: NavigationManager
    val dismissalCoordinator: LinkDismissalCoordinator
    val linkLaunchMode: LinkLaunchMode
    val autocompleteLauncher: AutocompleteLauncher

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun configuration(configuration: LinkConfiguration): Builder

        @BindsInstance
        fun publishableKeyProvider(@Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String): Builder

        @BindsInstance
        fun stripeAccountIdProvider(@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?): Builder

        @BindsInstance
        fun paymentElementCallbackIdentifier(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String
        ): Builder

        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        @BindsInstance
        fun statusBarColor(@Named(STATUS_BAR_COLOR) statusBarColor: Int?): Builder

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun startWithVerificationDialog(
            @Named(START_WITH_VERIFICATION_DIALOG) startWithVerificationDialog: Boolean
        ): Builder

        @BindsInstance
        fun linkLaunchMode(linkLaunchMode: LinkLaunchMode): Builder

        @BindsInstance
        fun linkAccountUpdate(linkAccountUpdate: LinkAccountUpdate.Value): Builder

        fun build(): NativeLinkComponent
    }
}
