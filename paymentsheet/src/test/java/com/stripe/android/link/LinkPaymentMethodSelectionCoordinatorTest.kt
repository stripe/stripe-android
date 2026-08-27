package com.stripe.android.link

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkPaymentMethodSelectionCoordinator.Action
import com.stripe.android.link.LinkPaymentMethodSelectionCoordinator.SelectionUpdateReason
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class LinkPaymentMethodSelectionCoordinatorTest {

    @Test
    fun `launchIfEligible presents Link for an existing Link selection`() = runScenario {
        val didLaunch = coordinator.launchIfEligible(
            launcher = linkLauncherScenario.launcher,
            paymentMethodMetadata = paymentMethodMetadata,
            selection = LINK_SELECTION,
            hasDeclinedLink2FA = false,
            statusBarColor = STATUS_BAR_COLOR,
        )

        assertThat(didLaunch).isTrue()
        val call = linkLauncherScenario.presentCalls.awaitItem()
        assertThat(call.paymentMethodMetadata).isEqualTo(paymentMethodMetadata)
        assertThat(call.linkAccount).isEqualTo(TestFactory.LINK_ACCOUNT)
        assertThat(call.linkExpressMode).isEqualTo(LinkExpressMode.ENABLED)
        assertThat(call.launchMode).isEqualTo(
            LinkLaunchMode.PaymentMethodSelection(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD)
        )
        assertThat(call.statusBarColor).isEqualTo(STATUS_BAR_COLOR)
    }

    @Test
    fun `launchIfEligible does not present Link after 2FA is declined`() = runScenario {
        val didLaunch = coordinator.launchIfEligible(
            launcher = linkLauncherScenario.launcher,
            paymentMethodMetadata = paymentMethodMetadata,
            selection = LINK_SELECTION,
            hasDeclinedLink2FA = true,
            statusBarColor = null,
        )

        assertThat(didLaunch).isFalse()
        linkLauncherScenario.presentCalls.expectNoEvents()
    }

    @Test
    fun `launchIfEligible does not present Link without an account`() = runScenario(
        linkAccountUpdate = LinkAccountUpdate.Value(null),
    ) {
        val didLaunch = coordinator.launchIfEligible(
            launcher = linkLauncherScenario.launcher,
            paymentMethodMetadata = paymentMethodMetadata,
            selection = LINK_SELECTION,
            hasDeclinedLink2FA = false,
            statusBarColor = null,
        )

        assertThat(didLaunch).isFalse()
        linkLauncherScenario.presentCalls.expectNoEvents()
    }

    @Test
    fun `launchIfEligible does not present Link when RUX is disabled`() = runScenario(
        showRuxInFlowController = false,
    ) {
        val didLaunch = coordinator.launchIfEligible(
            launcher = linkLauncherScenario.launcher,
            paymentMethodMetadata = paymentMethodMetadata,
            selection = LINK_SELECTION,
            hasDeclinedLink2FA = false,
            statusBarColor = null,
        )

        assertThat(didLaunch).isFalse()
        linkLauncherScenario.presentCalls.expectNoEvents()
    }

    @Test
    fun `completed result updates Link selection and account metadata`() = runScenario(
        paymentMethodMetadata = paymentMethodMetadata(LinkState.LoginState.LoggedOut),
    ) {
        val updatedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(id = "csmrpd_updated"),
            collectedCvc = null,
            billingPhone = null,
        )

        val outcome = coordinator.handleResult(
            result = LinkActivityResult.Completed(
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                selectedPayment = updatedPayment,
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            customer = null,
            selection = LINK_SELECTION,
        )

        val action = outcome.action as Action.UpdateSelection
        assertThat((action.selection as PaymentSelection.Link).selectedPayment).isEqualTo(updatedPayment)
        assertThat(action.reason).isEqualTo(SelectionUpdateReason.Completed)
        assertThat(outcome.updatedPaymentMethodMetadata?.linkState?.loginState)
            .isEqualTo(LinkState.LoginState.LoggedIn)
        assertThat(outcome.suppressFutureLinkLaunch).isFalse()
    }

    @Test
    fun `back from Link verification suppresses future launch`() = runScenario(
        linkAccountUpdate = LinkAccountUpdate.Value(verificationStartedAccount()),
    ) {
        val outcome = coordinator.handleResult(
            result = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.Value(verificationStartedAccount()),
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            customer = null,
            selection = LINK_SELECTION,
        )

        assertThat(outcome.action).isEqualTo(Action.Dismiss)
        assertThat(outcome.suppressFutureLinkLaunch).isTrue()
    }

    @Test
    fun `back without selected Link payment shows payment options`() = runScenario {
        val outcome = coordinator.handleResult(
            result = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            customer = null,
            selection = LINK_SELECTION.copy(selectedPayment = null),
        )

        assertThat(outcome.action).isEqualTo(Action.ShowPaymentOptions)
    }

    @Test
    fun `logout result selects the default saved payment method`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true,
            linkState = linkState(),
        ),
        customer = createCustomerState(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            defaultPaymentMethodId = PaymentMethodFixtures.CARD_PAYMENT_METHOD.id,
        ),
    ) {
        val outcome = coordinator.handleResult(
            result = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.Value(null),
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            customer = customer,
            selection = LINK_SELECTION,
        )

        val action = outcome.action as Action.UpdateSelection
        assertThat(action.selection).isEqualTo(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )
        assertThat(action.reason).isEqualTo(SelectionUpdateReason.LoggedOut)
        assertThat(outcome.updatedPaymentMethodMetadata?.linkState?.loginState)
            .isEqualTo(LinkState.LoginState.LoggedOut)
    }

    @Test
    fun `pay another way result shows payment options`() = runScenario {
        val outcome = coordinator.handleResult(
            result = LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.PayAnotherWay,
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            customer = null,
            selection = LINK_SELECTION,
        )

        assertThat(outcome.action).isEqualTo(Action.ShowPaymentOptions)
    }

    @Test
    fun `failed result dismisses Link`() = runScenario {
        val outcome = coordinator.handleResult(
            result = LinkActivityResult.Failed(
                error = IllegalStateException("Link failed"),
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            customer = null,
            selection = LINK_SELECTION,
        )

        assertThat(outcome.action).isEqualTo(Action.Dismiss)
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = paymentMethodMetadata(),
        customer: CustomerState? = null,
        linkAccountUpdate: LinkAccountUpdate.Value = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
        showRuxInFlowController: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle()).apply {
            set(linkAccountUpdate)
        }
        val linkGate = FakeLinkGate().apply {
            setShowRuxInFlowController(showRuxInFlowController)
        }
        RecordingLinkPaymentLauncher.test {
            Scenario(
                coordinator = LinkPaymentMethodSelectionCoordinator(
                    linkAccountHolder = linkAccountHolder,
                    linkGateFactory = FakeLinkGate.Factory(linkGate),
                ),
                paymentMethodMetadata = paymentMethodMetadata,
                customer = customer,
                linkLauncherScenario = this,
            ).block()
        }
    }

    private data class Scenario(
        val coordinator: LinkPaymentMethodSelectionCoordinator,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val customer: CustomerState?,
        val linkLauncherScenario: RecordingLinkPaymentLauncher.Scenario,
    )

    private companion object {
        const val STATUS_BAR_COLOR = 0x123456

        val LINK_SELECTION = PaymentSelection.Link(
            brand = LinkBrand.Link,
            selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = null,
                billingPhone = null,
            ),
        )

        fun paymentMethodMetadata(
            loginState: LinkState.LoginState = LinkState.LoginState.LoggedIn,
        ): PaymentMethodMetadata {
            return PaymentMethodMetadataFactory.create(linkState = linkState(loginState))
        }

        fun linkState(
            loginState: LinkState.LoginState = LinkState.LoginState.LoggedIn,
        ): LinkState {
            return LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = loginState,
                signupMode = null,
            )
        }

        fun verificationStartedAccount() = TestFactory.LINK_ACCOUNT.copy(
            consumerSession = TestFactory.CONSUMER_SESSION.copy(
                verificationSessions = listOf(TestFactory.VERIFICATION_STARTED_SESSION),
                currentAuthenticationLevel = ConsumerSession.AuthenticationLevel.NotAuthenticated,
                minimumAuthenticationLevel = ConsumerSession.AuthenticationLevel.OneFactorAuthentication,
            )
        )
    }
}
