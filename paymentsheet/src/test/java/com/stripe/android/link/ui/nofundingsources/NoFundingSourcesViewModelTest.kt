package com.stripe.android.link.ui.nofundingsources

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NoFundingSourcesViewModelTest {

    @Test
    fun `viewState shows pay another way when user can continue without Link`() = runScenario(
        linkLaunchMode = LinkLaunchMode.Full(showSecondaryButton = true),
    ) {
        assertThat(viewModel.viewState.linkBrand).isEqualTo(LinkBrand.Link)
        assertThat(viewModel.viewState.payAnotherWayLabel)
            .isEqualTo(R.string.stripe_wallet_pay_another_way.resolvableString)
    }

    @Test
    fun `viewState hides pay another way when user can't continue without Link`() = runScenario(
        linkLaunchMode = LinkLaunchMode.PaymentMethodSelection(
            selectedPayment = null,
            canContinueWithoutLink = false,
        ),
    ) {
        assertThat(viewModel.viewState.payAnotherWayLabel).isNull()
    }

    @Test
    fun `onPayAnotherWayClicked dismisses with pay another way reason`() = runScenario {
        viewModel.onPayAnotherWayClicked()

        val result = dismissWithResultCalls.awaitItem() as LinkActivityResult.Canceled
        assertThat(result.reason).isEqualTo(LinkActivityResult.Canceled.Reason.PayAnotherWay)
        assertThat(result.linkAccountUpdate).isEqualTo(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
    }

    @Test
    fun `onLogInToAnotherAccountClicked logs in to another account without dismissing`() = runScenario {
        viewModel.onLogInToAnotherAccountClicked()

        logInToAnotherAccountCalls.awaitItem()
    }

    private fun runScenario(
        linkLaunchMode: LinkLaunchMode = LinkLaunchMode.Full(showSecondaryButton = true),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val dismissWithResultCalls = Turbine<LinkActivityResult>()
        val logInToAnotherAccountCalls = Turbine<Unit>()
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))

        val viewModel = NoFundingSourcesViewModel(
            linkAccount = TestFactory.LINK_ACCOUNT,
            configuration = TestFactory.LINK_CONFIGURATION,
            linkLaunchMode = linkLaunchMode,
            linkAccountManager = linkAccountManager,
            dismissWithResult = { dismissWithResultCalls.add(it) },
            logInToAnotherAccount = { logInToAnotherAccountCalls.add(Unit) },
        )

        Scenario(
            viewModel = viewModel,
            dismissWithResultCalls = dismissWithResultCalls,
            logInToAnotherAccountCalls = logInToAnotherAccountCalls,
        ).block()

        dismissWithResultCalls.ensureAllEventsConsumed()
        logInToAnotherAccountCalls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val viewModel: NoFundingSourcesViewModel,
        val dismissWithResultCalls: Turbine<LinkActivityResult>,
        val logInToAnotherAccountCalls: Turbine<Unit>,
    )
}
