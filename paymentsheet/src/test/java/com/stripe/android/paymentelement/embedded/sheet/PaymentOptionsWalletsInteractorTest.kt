package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.WalletLocation
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentOptionsWalletsInteractorTest {
    @Test
    fun `direct form places both wallets in header`() = runScenario {
        interactor.walletsState.test {
            val state = awaitItem()!!
            assertThat(state.googlePay(WalletLocation.HEADER)).isNotNull()
            assertThat(state.link(WalletLocation.HEADER)).isNotNull()
        }
    }

    @Test
    fun `payment options list places Link in header and Google Pay inline`() = runScenario(
        paymentMethodMetadata = createMetadata(paymentMethodTypes = listOf("card", "cashapp")),
    ) {
        interactor.walletsState.test {
            val state = awaitItem()!!
            assertThat(state.link(WalletLocation.HEADER)).isNotNull()
            assertThat(state.googlePay(WalletLocation.INLINE)).isNotNull()
        }
    }

    @Test
    fun `direct form wallet placement remains stable after saved method is added`() = runScenario {
        customerStateHolder.setCustomerState(
            PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = PaymentMethodFixtures.createCards(1),
            )
        )

        assertThat(interactor.showsDirectForm).isTrue()
        assertThat(interactor.walletsState.value!!.googlePay(WalletLocation.HEADER)).isNotNull()
        assertThat(interactor.walletsState.value!!.googlePay(WalletLocation.INLINE)).isNull()
    }

    @Test
    fun `payment options list wallet placement remains stable after saved method is removed`() = runScenario(
        initialCustomerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = PaymentMethodFixtures.createCards(1),
        ),
    ) {
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)

        assertThat(interactor.showsDirectForm).isFalse()
        assertThat(interactor.walletsState.value!!.googlePay(WalletLocation.HEADER)).isNull()
        assertThat(interactor.walletsState.value!!.googlePay(WalletLocation.INLINE)).isNotNull()
    }

    @Test
    fun `wallet state updates when Link brand changes`() = runScenario {
        interactor.walletsState.test {
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.linkBrand).isEqualTo(LinkBrand.Link)

            linkAccountHolder.set(
                LinkAccountUpdate.Value(
                    account = LinkAccount(TestFactory.CONSUMER_SESSION.copy(linkBrand = LinkBrand.Onelink)),
                    lastUpdateReason = null,
                )
            )

            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.linkBrand).isEqualTo(LinkBrand.Onelink)
        }
    }

    @Test
    fun `Google Pay click selects Google Pay and continues`() = runScenario {
        interactor.walletsState.value!!.onGooglePayPressed()

        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.GooglePay)
        assertThat(continueCoordinator.onContinueCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `Link click selects current Link brand and continues`() = runScenario {
        linkAccountHolder.set(
            LinkAccountUpdate.Value(
                account = LinkAccount(TestFactory.CONSUMER_SESSION.copy(linkBrand = LinkBrand.Onelink)),
                lastUpdateReason = null,
            )
        )

        interactor.walletsState.value!!.onLinkPressed()

        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.Link(LinkBrand.Onelink))
        assertThat(continueCoordinator.onContinueCalls.awaitItem()).isEqualTo(Unit)
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = createMetadata(paymentMethodTypes = listOf("card")),
        initialCustomerState: CustomerState? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
        )
        initialCustomerState?.let(customerStateHolder::setCustomerState)
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val continueCoordinator = FakeSheetActivityContinueCoordinator()
        val interactor = PaymentOptionsWalletsInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            linkAccountHolder = linkAccountHolder,
            continueCoordinator = continueCoordinator,
        )

        Scenario(
            interactor = interactor,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            linkAccountHolder = linkAccountHolder,
            continueCoordinator = continueCoordinator,
        ).block()

        continueCoordinator.validate()
    }

    private data class Scenario(
        val interactor: PaymentOptionsWalletsInteractor,
        val customerStateHolder: DefaultCustomerStateHolder,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val linkAccountHolder: LinkAccountHolder,
        val continueCoordinator: FakeSheetActivityContinueCoordinator,
    )

    private companion object {
        fun createMetadata(paymentMethodTypes: List<String>): PaymentMethodMetadata {
            return PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = paymentMethodTypes,
                ),
                availableWallets = WalletType.entries,
                isGooglePayReady = true,
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION,
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                ),
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
            )
        }
    }
}
