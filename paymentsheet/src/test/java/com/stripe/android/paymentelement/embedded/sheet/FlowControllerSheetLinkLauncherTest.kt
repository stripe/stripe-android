package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.analytics.FakeLinkAnalyticsHelper
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.utils.FakeLinkStore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

internal class FlowControllerSheetLinkLauncherTest {

    @Test
    fun `onLinkActivityResult with Canceled does nothing`() = runScenario {
        linkLauncher.onLinkResult(LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None))

        assertThat(selectionHolder.selection.value).isNull()
    }

    @Test
    fun `onLinkActivityResult with Failed sets error`() = runScenario {
        linkLauncher.onLinkResult(
            LinkActivityResult.Failed(
                error = Exception("link failed"),
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `onLinkActivityResult with Completed continues with the Link selection`() = runScenario {
        val linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)

        linkLauncher.onLinkResult(
            LinkActivityResult.Completed(
                linkAccountUpdate = linkAccountUpdate,
                selectedPayment = null,
            )
        )

        val selection = selectionHolder.selection.value as PaymentSelection.Link
        assertThat(selection.brand).isEqualTo(LinkBrand.Link)
        assertThat(selection.selectedPayment).isNull()
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isEqualTo(linkAccountUpdate.account)
        assertThat(continueCoordinator.onContinueCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `onLinkActivityResult with PaymentMethodObtained sets error`() = runScenario {
        linkLauncher.onLinkResult(
            LinkActivityResult.PaymentMethodObtained(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isNotNull()
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val stateHolder = FakeSheetActivityStateHolder()
        val continueCoordinator = FakeSheetActivityContinueCoordinator()
        val linkLauncher = FlowControllerSheetLinkLauncher(
            linkAnalyticsComponentFactory = object : LinkAnalyticsComponent.Factory {
                override fun create() = object : LinkAnalyticsComponent {
                    override val linkAnalyticsHelper = FakeLinkAnalyticsHelper()
                }
            },
            paymentElementCallbackIdentifier = "FlowControllerSheetLinkLauncherTest",
            linkActivityContract = mock(),
            linkStore = FakeLinkStore(),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION,
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                ),
            ),
            linkAccountHolder = linkAccountHolder,
            selectionHolder = selectionHolder,
            stateHolder = stateHolder,
            continueCoordinator = continueCoordinator,
        )

        Scenario(
            linkLauncher = linkLauncher,
            selectionHolder = selectionHolder,
            linkAccountHolder = linkAccountHolder,
            stateHolder = stateHolder,
            continueCoordinator = continueCoordinator,
        ).block()

        stateHolder.validate()
        continueCoordinator.validate()
    }

    private class Scenario(
        val linkLauncher: FlowControllerSheetLinkLauncher,
        val selectionHolder: EmbeddedSelectionHolder,
        val linkAccountHolder: LinkAccountHolder,
        val stateHolder: FakeSheetActivityStateHolder,
        val continueCoordinator: FakeSheetActivityContinueCoordinator,
    )
}
