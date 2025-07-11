package com.stripe.android.link

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.Logger
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.FakeLinkAuth
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.utils.TestNavigationManager
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.addresselement.AutocompleteLauncher
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteLauncher
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import org.mockito.kotlin.mock

internal class FakeNativeLinkComponent(
    override val linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
    override val configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
    override val linkEventsReporter: LinkEventsReporter = FakeLinkEventsReporter(),
    override val logger: Logger = FakeLogger(),
    override val linkConfirmationHandlerFactory: LinkConfirmationHandler.Factory = LinkConfirmationHandler.Factory {
        FakeLinkConfirmationHandler()
    },
    override val webLinkActivityContract: WebLinkActivityContract = mock(),
    override val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory =
        NullCardAccountRangeRepositoryFactory,
    override val viewModel: LinkActivityViewModel = mock(),
    override val errorReporter: ErrorReporter = FakeErrorReporter(),
    override val linkAuth: LinkAuth = FakeLinkAuth(),
    override val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    override val eventReporter: EventReporter = FakeEventReporter(),
    override val navigationManager: NavigationManager = TestNavigationManager(),
    override val dismissalCoordinator: LinkDismissalCoordinator = RealLinkDismissalCoordinator(),
    override val linkLaunchMode: LinkLaunchMode = LinkLaunchMode.Full,
    override val autocompleteLauncher: AutocompleteLauncher = TestAutocompleteLauncher.noOp(),
) : NativeLinkComponent
