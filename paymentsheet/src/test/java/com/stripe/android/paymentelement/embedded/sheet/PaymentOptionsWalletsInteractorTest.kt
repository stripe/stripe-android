package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
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

        assertThat(interactor.canShowDirectForm).isTrue()
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

        assertThat(interactor.canShowDirectForm).isFalse()
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
    fun `wallet buttons are disabled while processing and enabled afterwards`() = runScenario {
        interactor.walletsState.test {
            assertThat(awaitItem()!!.buttonsEnabled).isTrue()

            sheetActivityStateHolder.updateState { it.copy(isProcessing = true) }
            assertThat(awaitItem()!!.buttonsEnabled).isFalse()

            sheetActivityStateHolder.updateState { it.copy(isProcessing = false) }
            assertThat(awaitItem()!!.buttonsEnabled).isTrue()
        }
    }

    @Test
    fun `wallet clicks cannot replace the selection during processing`() = runScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        val state = interactor.walletsState.value!!
        sheetActivityStateHolder.updateState { it.copy(isProcessing = true) }

        state.onGooglePayPressed()
        state.onLinkPressed()

        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        continueCoordinator.onContinueCalls.expectNoEvents()
    }

    @Test
    fun `Link header follows account email when default payment details are disabled`() = runScenario {
        interactor.walletsState.test {
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.state).isEqualTo(LinkButtonState.Default)

            linkAccountHolder.set(LinkAccountUpdate.Value(account = LINK_ACCOUNT_WITH_PAYMENT, lastUpdateReason = null))
            val state = awaitItem()!!.link(WalletLocation.HEADER)!!.state
            assertThat(state).isEqualTo(LinkButtonState.Email(LINK_ACCOUNT_WITH_PAYMENT.email))

            linkAccountHolder.set(LinkAccountUpdate.Value(account = null, lastUpdateReason = null))
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.state).isEqualTo(LinkButtonState.Default)
        }
    }

    @Test
    fun `Link header shows default payment details when enabled`() = runScenario(
        paymentMethodMetadata = createMetadata(
            paymentMethodTypes = listOf("card"),
            enableDisplayableDefaultValues = true,
        ),
    ) {
        interactor.walletsState.test {
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.state).isEqualTo(LinkButtonState.Default)

            linkAccountHolder.set(LinkAccountUpdate.Value(account = LINK_ACCOUNT_WITH_PAYMENT, lastUpdateReason = null))

            val state = awaitItem()!!.link(WalletLocation.HEADER)!!.state
            assertThat(state).isInstanceOf(LinkButtonState.DefaultPayment::class.java)
            assertThat((state as LinkButtonState.DefaultPayment).paymentUI.last4).isEqualTo("4242")
        }
    }

    @Test
    fun `Link selection without a selected payment allows default payment details`() = runScenario(
        paymentMethodMetadata = createMetadata(
            paymentMethodTypes = listOf("card"),
            enableDisplayableDefaultValues = true,
        ),
        initialSelection = PaymentSelection.Link(LinkBrand.Link),
    ) {
        linkAccountHolder.set(LinkAccountUpdate.Value(account = LINK_ACCOUNT_WITH_PAYMENT, lastUpdateReason = null))

        interactor.walletsState.test {
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.state)
                .isInstanceOf(LinkButtonState.DefaultPayment::class.java)
        }
    }

    @Test
    fun `Link selection with a selected payment disables default payment details`() = runScenario(
        paymentMethodMetadata = createMetadata(
            paymentMethodTypes = listOf("card"),
            enableDisplayableDefaultValues = true,
        ),
        initialSelection = LINK_SELECTION_WITH_PAYMENT,
    ) {
        linkAccountHolder.set(LinkAccountUpdate.Value(account = LINK_ACCOUNT_WITH_PAYMENT, lastUpdateReason = null))

        interactor.walletsState.test {
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.state)
                .isEqualTo(LinkButtonState.Email(LINK_ACCOUNT_WITH_PAYMENT.email))
        }
    }

    @Test
    fun `Link default payment details update when only selection changes`() = runScenario(
        paymentMethodMetadata = createMetadata(
            paymentMethodTypes = listOf("card"),
            enableDisplayableDefaultValues = true,
        ),
    ) {
        linkAccountHolder.set(LinkAccountUpdate.Value(account = LINK_ACCOUNT_WITH_PAYMENT, lastUpdateReason = null))

        interactor.walletsState.test {
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.state)
                .isInstanceOf(LinkButtonState.DefaultPayment::class.java)

            selectionHolder.setSelection(LINK_SELECTION_WITH_PAYMENT)
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.state)
                .isEqualTo(LinkButtonState.Email(LINK_ACCOUNT_WITH_PAYMENT.email))

            selectionHolder.setSelection(PaymentSelection.Link(LinkBrand.Link))
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.state)
                .isInstanceOf(LinkButtonState.DefaultPayment::class.java)

            selectionHolder.setSelection(LINK_SELECTION_WITH_PAYMENT)
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.state)
                .isEqualTo(LinkButtonState.Email(LINK_ACCOUNT_WITH_PAYMENT.email))

            selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            assertThat(awaitItem()!!.link(WalletLocation.HEADER)!!.state)
                .isInstanceOf(LinkButtonState.DefaultPayment::class.java)
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

    @Test
    fun `hidden Link is not shown in header or inline`() = runScenario(
        paymentMethodMetadata = createMetadata(paymentMethodTypes = listOf("card", "cashapp")),
        launchMode = EmbeddedLaunchMode.PaymentOptions(visibleWallets = listOf(WalletType.GooglePay)),
    ) {
        val state = interactor.walletsState.value!!
        assertThat(state.link(WalletLocation.HEADER)).isNull()
        assertThat(state.link(WalletLocation.INLINE)).isNull()
        assertThat(state.googlePay(WalletLocation.INLINE)).isNotNull()
    }

    @Test
    fun `hidden Google Pay is not shown in header or inline`() = runScenario(
        paymentMethodMetadata = createMetadata(paymentMethodTypes = listOf("card", "cashapp")),
        launchMode = EmbeddedLaunchMode.PaymentOptions(visibleWallets = listOf(WalletType.Link)),
    ) {
        val state = interactor.walletsState.value!!
        assertThat(state.googlePay(WalletLocation.HEADER)).isNull()
        assertThat(state.googlePay(WalletLocation.INLINE)).isNull()
        assertThat(state.link(WalletLocation.HEADER)).isNotNull()
    }

    @Test
    fun `hidden wallets are not shown in direct form header`() = runScenario(
        launchMode = EmbeddedLaunchMode.PaymentOptions(visibleWallets = emptyList()),
    ) {
        assertThat(interactor.walletsState.value).isNull()
    }

    @Test
    fun `all wallets are shown outside of payment options launch mode`() = runScenario(
        launchMode = EmbeddedLaunchMode.Manage,
    ) {
        val state = interactor.walletsState.value!!
        assertThat(state.googlePay(WalletLocation.HEADER)).isNotNull()
        assertThat(state.link(WalletLocation.HEADER)).isNotNull()
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = createMetadata(paymentMethodTypes = listOf("card")),
        initialCustomerState: CustomerState? = null,
        initialSelection: PaymentSelection? = null,
        launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.PaymentOptions(visibleWallets = WalletType.entries),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        selectionHolder.setSelection(initialSelection)
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
        )
        initialCustomerState?.let(customerStateHolder::setCustomerState)
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val continueCoordinator = FakeSheetActivityContinueCoordinator()
        val sheetActivityStateHolder = FakeSheetActivityStateHolder()
        val interactor = PaymentOptionsWalletsInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            launchMode = launchMode,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            linkAccountHolder = linkAccountHolder,
            continueCoordinator = continueCoordinator,
            sheetActivityStateHolder = sheetActivityStateHolder,
        )

        Scenario(
            interactor = interactor,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            linkAccountHolder = linkAccountHolder,
            continueCoordinator = continueCoordinator,
            sheetActivityStateHolder = sheetActivityStateHolder,
        ).block()

        continueCoordinator.validate()
        sheetActivityStateHolder.validate()
    }

    private data class Scenario(
        val interactor: PaymentOptionsWalletsInteractor,
        val customerStateHolder: DefaultCustomerStateHolder,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val linkAccountHolder: LinkAccountHolder,
        val continueCoordinator: FakeSheetActivityContinueCoordinator,
        val sheetActivityStateHolder: FakeSheetActivityStateHolder,
    )

    private companion object {
        val LINK_SELECTION_WITH_PAYMENT = PaymentSelection.Link(
            brand = LinkBrand.Link,
            selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                collectedCvc = null,
                billingPhone = null,
            ),
        )

        val LINK_ACCOUNT_WITH_PAYMENT = TestFactory.LINK_ACCOUNT.copy(
            displayablePaymentDetails = DisplayablePaymentDetails(
                defaultCardBrand = "visa",
                last4 = "4242",
                defaultPaymentType = "CARD",
                numberOfSavedPaymentDetails = 1L,
            ),
        )

        fun createMetadata(
            paymentMethodTypes: List<String>,
            enableDisplayableDefaultValues: Boolean = false,
        ): PaymentMethodMetadata {
            return PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = paymentMethodTypes,
                ),
                availableWallets = WalletType.entries,
                isGooglePayReady = true,
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION.copy(
                        enableDisplayableDefaultValuesInEce = enableDisplayableDefaultValues,
                    ),
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                ),
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
            )
        }
    }
}
