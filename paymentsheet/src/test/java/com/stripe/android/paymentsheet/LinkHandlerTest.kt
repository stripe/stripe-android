package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_SELECTION
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkHandlerTest {
    @Test
    fun `Prepares state correctly for logged out user`() = runLinkTest {
        accountStatusFlow.emit(AccountStatus.SignedOut)
        handler.setupLink(
            createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse
            )
        )
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(savedStateHandle.get<PaymentSelection>(SAVE_SELECTION)).isNull()
    }

    @Test
    fun `Prepares state correctly with SFU signup mode`() = runLinkTest {
        accountStatusFlow.emit(AccountStatus.SignedOut)
        handler.setupLink(
            createLinkState(
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.AlongsideSaveForFutureUse
            )
        )
        assertThat(handler.isLinkEnabled.first()).isTrue()
        assertThat(savedStateHandle.get<PaymentSelection>(SAVE_SELECTION)).isNull()
    }
}

private fun runLinkTest(
    accountStatusFlow: MutableSharedFlow<AccountStatus> = MutableSharedFlow(replay = 1),
    linkConfiguration: LinkConfiguration = defaultLinkConfiguration(),
    attachNewCardToAccountResult: Result<LinkPaymentDetails>? = null,
    testBlock: suspend LinkTestData.() -> Unit
): Unit = runTest {
    val linkConfigurationCoordinator = mock<LinkConfigurationCoordinator>()
    val savedStateHandle = SavedStateHandle()
    val linkAnalyticsHelper = mock<LinkAnalyticsHelper>()
    val linkStore = mock<LinkStore>()
    val handler = LinkHandler(
        linkConfigurationCoordinator = linkConfigurationCoordinator,
    )

    val testScope = this
    turbineScope {
        whenever(linkConfigurationCoordinator.getAccountStatusFlow(eq(linkConfiguration))).thenReturn(accountStatusFlow)
        whenever(linkConfigurationCoordinator.attachNewCardToAccount(eq(linkConfiguration), any())).thenReturn(
            attachNewCardToAccountResult
        )

        with(
            LinkTestDataImpl(
                testScope = testScope,
                handler = handler,
                linkConfigurationCoordinator = linkConfigurationCoordinator,
                linkStore = linkStore,
                savedStateHandle = savedStateHandle,
                configuration = linkConfiguration,
                accountStatusFlow = accountStatusFlow,
                linkAnalyticsHelper = linkAnalyticsHelper,
            )
        ) {
            testBlock()
        }
    }
}

private fun LinkTestData.createLinkState(
    loginState: LinkState.LoginState,
    signupMode: LinkSignupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
): LinkState {
    return LinkState(
        loginState = loginState,
        signupMode = signupMode,
        configuration = configuration,
    )
}

private fun defaultLinkConfiguration(
    linkFundingSources: List<String> = emptyList(),
): LinkConfiguration {
    return TestFactory.LINK_CONFIGURATION.copy(
        stripeIntent = PaymentIntentFactory.create(
            linkFundingSources = linkFundingSources,
        )
    )
}

private class LinkTestDataImpl(
    override val testScope: TestScope,
    override val handler: LinkHandler,
    override val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    override val linkStore: LinkStore,
    override val savedStateHandle: SavedStateHandle,
    override val configuration: LinkConfiguration,
    override val accountStatusFlow: MutableSharedFlow<AccountStatus>,
    override val linkAnalyticsHelper: LinkAnalyticsHelper,
) : LinkTestData

private interface LinkTestData {
    val testScope: TestScope
    val handler: LinkHandler
    val linkConfigurationCoordinator: LinkConfigurationCoordinator
    val linkStore: LinkStore
    val savedStateHandle: SavedStateHandle
    val configuration: LinkConfiguration
    val accountStatusFlow: MutableSharedFlow<AccountStatus>
    val linkAnalyticsHelper: LinkAnalyticsHelper
}
