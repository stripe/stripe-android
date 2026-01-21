package com.stripe.android.link.injection

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.DEFAULT_ACCOUNT_RANGE_REPO
import com.stripe.android.cards.FUNDING_ACCOUNT_RANGE_REPO
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.WebLinkActivityContract
import com.stripe.android.link.WebLinkAuthChannel
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.ui.oauth.OAuthConsentViewModelComponent
import com.stripe.android.link.ui.wallet.AddPaymentMethodOptions
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.RequestSurface
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.injection.DefaultConfirmationModule
import com.stripe.android.paymentelement.confirmation.intent.DefaultIntentConfirmationModule
import com.stripe.android.paymentelement.confirmation.link.LinkPassthroughConfirmationModule
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.addresselement.AutocompleteLauncher
import com.stripe.android.paymentsheet.analytics.EventReporter
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
        ApplicationIdModule::class,
        DefaultConfirmationModule::class,
        DefaultIntentConfirmationModule::class,
        LinkPassthroughConfirmationModule::class,
    ]
)
internal interface NativeLinkComponent {
    val linkAccountHolder: LinkAccountHolder
    val linkAccountManager: LinkAccountManager
    val configuration: LinkConfiguration
    val linkEventsReporter: LinkEventsReporter
    val errorReporter: ErrorReporter
    val logger: Logger
    val linkConfirmationHandlerFactory: LinkConfirmationHandler.Factory
    val webLinkActivityContract: WebLinkActivityContract

    @get:Named(DEFAULT_ACCOUNT_RANGE_REPO)
    val cardAccountRangeServiceFactory: CardAccountRangeService.Factory

    @get:Named(FUNDING_ACCOUNT_RANGE_REPO)
    val fundingCardAccountRangeServiceFactory: CardAccountRangeService.Factory
    val savedStateHandle: SavedStateHandle
    val viewModel: LinkActivityViewModel
    val eventReporter: EventReporter
    val navigationManager: NavigationManager
    val dismissalCoordinator: LinkDismissalCoordinator
    val linkLaunchMode: LinkLaunchMode
    val autocompleteLauncher: AutocompleteLauncher
    val addPaymentMethodOptionsFactory: AddPaymentMethodOptions.Factory
    val oAuthConsentViewModelComponentFactory: OAuthConsentViewModelComponent.Factory
    val webLinkAuthChannel: WebLinkAuthChannel
    val paymentMethodMetadata: PaymentMethodMetadata

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            configuration: LinkConfiguration,
            @BindsInstance
            paymentMethodMetadata: PaymentMethodMetadata,
            @BindsInstance
            @Named(PUBLISHABLE_KEY)
            publishableKeyProvider: () -> String,
            @BindsInstance
            @Named(STRIPE_ACCOUNT_ID)
            stripeAccountIdProvider: () -> String?,
            @BindsInstance
            @PaymentElementCallbackIdentifier
            paymentElementCallbackIdentifier: String,
            @BindsInstance
            context: Context,
            @BindsInstance
            savedStateHandle: SavedStateHandle,
            @BindsInstance
            @Named(STATUS_BAR_COLOR)
            statusBarColor: Int?,
            @BindsInstance
            application: Application,
            @BindsInstance
            @Named(LINK_EXPRESS_MODE)
            linkExpressMode: LinkExpressMode,
            @BindsInstance
            linkLaunchMode: LinkLaunchMode,
            @BindsInstance
            linkAccountUpdate: LinkAccountUpdate.Value,
            @BindsInstance
            requestSurface: RequestSurface,
        ): NativeLinkComponent
    }
}
